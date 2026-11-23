package play.libs.f;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import play.libs.F;

import java.time.Duration;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class EventStreamTest {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    @AfterAll static void shutdownExecutor() {
        assertThat(EXECUTOR.shutdownNow()).isEmpty();
    }

    @Test void singleThreadNextEventBeforePublish() {
        var event = new Object();
        var stream = new F.EventStream<>();

        var future = stream.nextEvent();
        assertThat(future).isNotDone();

        stream.publish(event);

        assertThat(future).isCompletedWithValue(event);
    }

    @Test void singleThreadPublishBeforeNextEvent() throws Exception {
        var events = ThreadLocalRandom.current().ints(5L).boxed().toList();
        var stream = new F.EventStream<Integer>();

        events.forEach(stream::publish);

        for (var event : events) {
            // each read consumes exactly one event from the queue
            assertThat(stream.nextEvent().get()).isEqualTo(event);
        }
    }

    @Timeout(10)
    @Test void concurrentPublishersAndSubscribers() throws Exception {
        int publisherCounts = 5;
        int eventsPerPublisher = 20;
        int subscriberCounts = 3;

        var startLatch = new CountDownLatch(1);
        var completionPublishersLatch = new CountDownLatch(publisherCounts);

        var publishedEvents = new ConcurrentLinkedQueue<String>();
        var receivedEvents = new ConcurrentLinkedQueue<String>();

        var stream = new F.EventStream<String>();

        for (int i = 0; i < subscriberCounts; i++) {
            EXECUTOR.submit(() -> {
                try {
                    startLatch.await();
                    // keep reading (i.e. consuming) events until the
                    // publishers are done: registration alone never removes
                    // an event from the queue, so each event handed out must
                    // be read back
                    while (completionPublishersLatch.getCount() > 0L) {
                        try {
                            receivedEvents.add(stream.nextEvent().get(1, TimeUnit.SECONDS));
                        } catch (TimeoutException e) {
                            // the queue is momentarily empty, keep polling
                            // while the publishers are still running
                        } catch (ExecutionException e) {
                            // stream tasks never complete exceptionally
                            throw new AssertionError(e);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        for (int i = 0; i < publisherCounts; i++) {
            final int publisherId = i;
            EXECUTOR.submit(() -> {
                try {
                    startLatch.await();
                    for (int eventId = 0; eventId < eventsPerPublisher; eventId++) {
                        var event = "Publisher-" + publisherId + "-Event-" + eventId;

                        stream.publish(event);

                        publishedEvents.add(event);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionPublishersLatch.countDown();
                }
            });
        }

        // start publishers and subscribers
        startLatch.countDown();

        // wait all publishers
        completionPublishersLatch.await();

        // terminal drain: read the remaining queued events; a pending future
        // (the queue is empty) ends the loop
        while (true) {
            CompletableFuture<String> event = stream.nextEvent();
            try {
                receivedEvents.add(event.get(1, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                break;
            }
        }

        // all events should be received
        assertThat(receivedEvents).containsAll(publishedEvents);
    }

    @Test void eventStreamNextEventDoesNotLoseEvents() throws Exception {
        var stream = new F.EventStream<Integer>();

        // subscribed before the first publish
        CompletableFuture<Integer> earlySubscriber = stream.nextEvent();
        assertThat(earlySubscriber).isNotDone();

        stream.publish(1);
        stream.publish(1);
        stream.publish(1);

        // every waiting subscriber is completed with the (still unconsumed) event
        assertThat(earlySubscriber).isDone();

        // each actual read consumes exactly one event
        assertThat(earlySubscriber.get()).isEqualTo(1);
        assertThat(stream.nextEvent().get()).isEqualTo(1);
        assertThat(stream.nextEvent().get()).isEqualTo(1);

        // all three events are consumed — no fourth delivery exists
        assertThat(stream.nextEvent()).isNotDone();
    }

    @Test void eventStaysInQueueUntilItIsRead() throws Exception {
        var stream = new F.EventStream<String>();

        stream.publish("event");

        var first = stream.nextEvent();
        var second = stream.nextEvent();

        // the event stays in the queue until one of the subscribers actually
        // reads it (isDone() checks delivery without consuming)
        assertThat(first).isDone();
        assertThat(second).isDone();

        assertThat(first.get()).isEqualTo("event");

        // once read, the event is consumed and is not delivered again
        assertThat(stream.nextEvent()).isNotDone();
    }

    @Test void eventIsConsumedOnlyByAnActualRead() throws Exception {
        var stream = new F.EventStream<String>();

        var pending = stream.nextEvent();
        assertThat(pending).isNotDone();

        stream.publish("event");
        // delivering the event to a waiting subscriber does not consume it
        // (isDone() checks the state without reading the value through join())
        assertThat(pending).isDone();

        // reading consumes the event — exactly what Promise.waitAny and
        // Promise.waitEither do internally through join() on the winner
        assertThat(pending.join()).isEqualTo("event");

        // regression for duplicated WebSocket messages: the consumed event
        // must not be handed out again
        assertThat(stream.nextEvent()).isNotDone();
    }

    @Test void publishDeliversEveryLiveSubscriberExactlyOnce() throws Exception {
        var stream = new F.EventStream<String>();

        var subscriber1 = stream.nextEvent();
        var subscriber2 = stream.nextEvent();

        stream.publish("hello");
        // delivery (isDone()) without a read must not consume the event
        assertThat(subscriber1).isDone();
        assertThat(subscriber2).isDone();

        // each subscriber read consumes its copy exactly once
        assertThat(subscriber1.join()).isEqualTo("hello");
        assertThat(subscriber2.join()).isEqualTo("hello");

        assertThat(stream.nextEvent()).isNotDone();
    }

}
