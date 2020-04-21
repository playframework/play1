import org.junit.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import play.*;
import play.test.*;
import play.jobs.*;
import play.libs.*;
import play.libs.F.*;

public class Promises extends UnitTest {

    public static class DoSomething extends play.jobs.Job<F.Option<String>> {

        long d;

        public DoSomething(long d) {
            this.d = d;
        }

        public F.Option<String> doJobWithResult() throws Exception {
            Thread.sleep(d);
            if(d > 200) {
                return F.Option.Some("-> " + d);
            }
            return F.Option.None();
        }

    }

    public static class DoSomething2 extends play.jobs.Job<String> {

        long d;

        public DoSomething2(long d) {
            this.d = d;
        }

        public String doJobWithResult() throws Exception {
            Thread.sleep(d);
            return "-> " + d;
        }

    }

    @Test
    public void waitAny() throws Exception {

        boolean p = false;
        for(String s : Promise.waitAny(new DoSomething(300).now(), new DoSomething(250).now()).get()) {
            assertEquals("-> 250", s);
            p = true;
        }
        assertTrue("Loop missed?", p);

        for(String s : Promise.waitAny(new DoSomething(100).now(), new DoSomething(250).now()).get()) {
            fail("Oops");
        }

    }

    @Test
    public void waitEither() throws Exception {

        F.Either<F.Option<String>,String> e = Promise.waitEither(new DoSomething(201).now(), new DoSomething2(300).now()).get();

        boolean p = false;
        for(F.Option<String> o : e._1) {
            for(String s : o) {
                assertEquals("-> 201", s);
                p = true;
            }
        }
        assertTrue("Loop missed?", p);

        for(String s : e._2) {
            fail("Oops");
        }

        e = Promise.waitEither(new DoSomething(201).now(), new DoSomething2(100).now()).get();

        for(F.Option<String> o : e._1) {
            for(String s : o) {
                fail("Oops");
            }
        }

        p = false;
        for(String s : e._2) {
            assertEquals("-> 100", s);
            p = true;
        }
        assertTrue("Loop missed?", p);

    }

    @Test
    public void waitAll() throws Exception {

        List<String> s = Promise.waitAll(new DoSomething2(200).now(), new DoSomething2(10).now()).get();
        assertEquals(2, s.size());
        assertEquals("-> 200", s.get(0));
        assertEquals("-> 10", s.get(1));

    }

    @Test
    public void wait2() throws Exception {

        F.Tuple<String, F.Option<String>> t = Promise.wait2(new DoSomething2(200).now(), new DoSomething(10).now()).get();

        assertEquals("-> 200", t._1);
        for(String s : t._2) {
            fail("Oops");
        }

    }

    @Test
    public void eventStream() throws Exception {

        final EventStream<String> stream = new EventStream<String>();

        new Thread() {

            public void run() {
                try {
                    Thread.sleep(300);
                    stream.publish("Héhé");
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }
            }

        }.start();

        for(int i=0; i<6; i++) {

            String eventReceived = Promise.waitAny(stream.nextEvent(), new DoSomething2(100).now()).get();
            System.out.println(i + " -> " + eventReceived);

            switch(i) {
                case 0:
                    assertEquals("-> 100", eventReceived);
                    Thread.sleep(500);
                    break;
                case 1:
                    assertEquals("Héhé", eventReceived);
                    break;
                case 2:
                    assertEquals("-> 100", eventReceived);
                    stream.publish("Coco");
                    stream.publish("Kiki");
                    break;
                case 3:
                    assertEquals("Coco", eventReceived);
                    break;
                case 4:
                    assertEquals("Kiki", eventReceived);
                    break;
                case 5:
                    assertEquals("-> 100", eventReceived);
                    break;
            }
        }

    }

    @Test
    public void bufferedEventStream() throws Exception {

        IndexedEvent.resetIdGenerator();
        final ArchivedEventStream<String> stream = new ArchivedEventStream<String>(5);

        new Thread() {

            public void run() {
                try {
                    Thread.sleep(300);
                    stream.publish("Héhé");
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }
            }

        }.start();

        long lastSeen = 0;

        for(int i=0; i<10; i++) {

            F.Either<List<IndexedEvent<String>>,String> eventReceived = Promise.waitEither(stream.nextEvents(lastSeen), new DoSomething2(100).now()).get();
            System.out.println(i + " -> " + eventReceived);

            switch(i) {
                case 0:
                    assertEquals("-> 100", eventReceived._2.get());
                    Thread.sleep(500);
                    break;
                case 1:
                    assertEquals(1, eventReceived._1.get().size());
                    assertEquals("Héhé", eventReceived._1.get().get(0).data);
                    assertEquals((Long)1L, eventReceived._1.get().get(0).id);
                    break;
                case 2:
                    assertEquals(1, eventReceived._1.get().size());
                    assertEquals("Héhé", eventReceived._1.get().get(0).data);
                    assertEquals((Long)1L, eventReceived._1.get().get(0).id);
                    lastSeen = eventReceived._1.get().get(0).id;
                    break;
                case 3:
                    assertEquals("-> 100", eventReceived._2.get());
                    stream.publish("Coco");
                    stream.publish("Kiki");
                    break;
                case 4:
                    assertEquals(2, eventReceived._1.get().size());
                    assertEquals("Coco", eventReceived._1.get().get(0).data);
                    assertEquals((Long)2L, eventReceived._1.get().get(0).id);
                    lastSeen = eventReceived._1.get().get(0).id;
                    break;
                case 5:
                    assertEquals(1, eventReceived._1.get().size());
                    assertEquals("Kiki", eventReceived._1.get().get(0).data);
                    assertEquals((Long)3L, eventReceived._1.get().get(0).id);
                    lastSeen = eventReceived._1.get().get(0).id;
                    break;
                case 6:
                    assertEquals("-> 100", eventReceived._2.get());
                    stream.publish("Boum");
                    stream.publish("Yop");
                    stream.publish("Paf");
                    break;
                case 7:
                    assertEquals(3, eventReceived._1.get().size());
                    assertEquals("Boum", eventReceived._1.get().get(0).data);
                    assertEquals((Long)4L, eventReceived._1.get().get(0).id);
                    lastSeen = 0;
                    break;
                case 8:
                    assertEquals(5, eventReceived._1.get().size());
                    assertEquals("Coco", eventReceived._1.get().get(0).data);
                    assertEquals((Long)2L, eventReceived._1.get().get(0).id);
                    lastSeen = 100;
                    break;
                case 9:
                    assertEquals("-> 100", eventReceived._2.get());
                    break;
            }
        }

    }

  @Test(expected = TimeoutException.class)
  public void waitAllTimeout() throws InterruptedException, ExecutionException, TimeoutException {
    Promise.waitAll(new DoSomething2(200).now(), new DoSomething2(200).now()).get(100, TimeUnit.MILLISECONDS);
  }

  @Test()
  public void waitAllNoTimeout() throws InterruptedException, ExecutionException, TimeoutException {
    Promise.waitAll(new DoSomething2(200).now(), new DoSomething2(200).now()).get(400, TimeUnit.MILLISECONDS);
  }

  @Test(expected = TimeoutException.class)
  public void waitForTimeout() throws InterruptedException, ExecutionException, TimeoutException {
    new DoSomething2(200).now().get(100, TimeUnit.MILLISECONDS);
  }

    /**
     * This is a regression test for Lighthouse #2086, whose root cause was race condition in Promise such
     * that, if a new "onRedeem" callback is registered at about the same time that the Promise is invoked,
     * the "onRedeem" callback could be called multiple times.
     * <p>
     * This broke assumptions in the Controllers.await() subsystem and caused a continuation to be resumed
     * multiple times.
     * </p>
     *
     * @throws InterruptedException
     *             if the test thread is interrupted while waiting.
     * @throws ExecutionException
     *             if the test thread is interrupted while waiting.
     */
    @Test
    public void promiseOnRedeemIsInvokedExactlyOnce() throws InterruptedException, ExecutionException {
        Thread testThread = Thread.currentThread();

        // Set up a pair of atomic longs to track the number of times that the onRedeem
        // callback is called from the invoker thread and the number of times it's called
        // from the callback registration thread.
        final AtomicLong hitsFromTestThread = new AtomicLong(0);
        final AtomicLong hitsFromExecutorService = new AtomicLong(0);

        // Create a thread pool executor on which we will invoke our promises.
        // This is a separate executor than the job pool to reduce execution
        // overhead and make it more likely that that we'll see the race condition.
        final ExecutorService executor = Executors.newFixedThreadPool(1);

        // The number of times to call Promise.onRedeem().
        final int totalCallbacksRegisteredOnPromise = 500;

        // Since we are testing for a race condition, we run the same test multiple times.
        // If, on any iteration of the loop, we find a promise that exhibits the bug,
        // we fail the test.  To guarantee that the test runs in a reasonable amount
        // of time regardless of the hardware, we run the test for a fixed amount
        // of time, rather than a fixed number of iterations.
        long totalNumberOfSecondsToExecuteTests = 2;
        long targetEndTimeNs = System.nanoTime() + totalNumberOfSecondsToExecuteTests * 1000 * 1000 * 1000;
        long i = 0;
        while (System.nanoTime() - targetEndTimeNs < 0) {
            // Create an action that, when invoked, increments an atomic integer.
            // This action should be invoked exactly as many times as it is passed to
            // Promise.onRedeem().
            AtomicInteger totalTimesPromiseIsInvoked = new AtomicInteger(0);
            F.Action<Promise> callback = new F.Action() {
                @Override
                public void invoke(Object result) {
                    // Note that the "onRedeem" action was invoked.
                    totalTimesPromiseIsInvoked.addAndGet(1);

                    // Note whether the promise was redeemed from the test thread
                    // or one of the executor service's threads to get some measurement
                    // on how likely it is that the race condition was exercised.
                    if (Thread.currentThread().equals(testThread)) {
                        hitsFromTestThread.incrementAndGet();
                    } else {
                        hitsFromExecutorService.incrementAndGet();
                    }
                }
            };

            // Create the unit under test.
            Promise smartPromise = new Promise();

            Thread.sleep(0); // surrender the rest of our time slice.

            // Invoke the promise from a different thread.
            Future executorFuture = executor.submit(new Callable() {
                @Override
                public Object call() throws InterruptedException {
                    smartPromise.invoke(null);
                    return null;
                }
            });

            // On this thread, register the "onRedeem" action.
            // We hope to do this at about the time that it's invoked from
            // the executor service's thread.  We do this many times to
            // make that more likely.
            for (int j = 0; j < totalCallbacksRegisteredOnPromise; j++) {
                smartPromise.onRedeem(callback);
            }

            // Wait for the promise to be invoked.
            executorFuture.get();

            // The onRedeem action should have been invoked exactly once for each time
            // it was registered.
            assertEquals(String.format("The %dth iteration failed", i), totalCallbacksRegisteredOnPromise,
                totalTimesPromiseIsInvoked.get());
            i++;
        }

        // We made it through all iterations without seeing the race condition.
        // This could be because no race condition exists, or it could be that the timing
        // of the test happens to make the race condition very unlikely.
        // The following info message is to help a test developer tune the test parameters to
        // maximize the chance of finding the race.  A well-tuned test will have called the
        // onRedeem callback about as many times from this thread as from the executor service.
        Logger.info("Hits from test thread: %d, hits from executor service: %d", hitsFromTestThread.get(),
            hitsFromExecutorService.get());
    }
}
