package play.libs.f;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import play.libs.F;

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

    @Test void singleThreadPublishBeforeNextEvent() {
        var events = ThreadLocalRandom.current().ints(5L).boxed().toList();
        var stream = new F.EventStream<Integer>();

        events.forEach(stream::publish);

        events.forEach(event ->
            assertThat(stream.nextEvent())
                .isCompletedWithValue(event)
        );
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
                    // read until publishers is working
                    while (completionPublishersLatch.getCount() > 0L) {
                        stream.nextEvent().whenComplete((v, e) -> receivedEvents.add(v));
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

        // wait all subscribers: if future is not done then events queue is empty
        while (stream.nextEvent().thenAccept(receivedEvents::add).isDone()) {
        }

        // all events should be received
        assertThat(receivedEvents).containsAll(publishedEvents);
    }

    @Test void eventStreamNextEventDoesNotLoseEvents() {
        var stream = new F.EventStream<Integer>();

        assertThat(stream.nextEvent()).isNotDone();

        stream.publish(1);
        stream.publish(1);
        stream.publish(1);

        F.Promise<Integer>[] promises = new F.Promise[] {
            stream.nextEvent(),
            stream.nextEvent(),
            stream.nextEvent()
        };

        for (F.Promise<Integer> promise : promises) {
            assertThat(promise).isCompletedWithValue(1);
        }
    }

}
