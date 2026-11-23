package play.libs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import play.libs.F.ArchivedEventStream;
import play.libs.F.EventStream;
import play.libs.F.IndexedEvent;

/**
 * @author olger
 *
 */
class ArchivedEventStreamTest {

    @BeforeEach void resetIdGenerator() {
        IndexedEvent.resetIdGenerator();
    }

    @Test void archiveShouldBeEmptyWhenNothingPublished() {
        ArchivedEventStream<String> stream = new ArchivedEventStream<>(10);
        assertThat(stream.archive()).isEmpty();
    }

    @Test void archiveShouldReturnAllPublishedData() {
        ArchivedEventStream<String> stream = new ArchivedEventStream<>(10);
        stream.publish("a");
        stream.publish("b");
        stream.publish("c");

        List<String> archive = stream.archive();
        assertThat(archive).containsExactly("a", "b", "c");
    }

    @Test void availableEventsShouldReturnAllWhenLastSeenIsZero() {
        ArchivedEventStream<String> stream = new ArchivedEventStream<>(10);
        stream.publish("a");
        stream.publish("b");

        assertThat(stream.availableEvents(0))
            .map(event -> event.data)
            .containsExactly("a", "b");
    }

    @Test void availableEventsShouldReturnOnlyEventsAfterLastSeen() {
        ArchivedEventStream<String> stream = new ArchivedEventStream<>(10);
        stream.publish("a"); // id=1
        stream.publish("b"); // id=2
        stream.publish("c"); // id=3

        assertThat(stream.availableEvents(1))
            .map(event -> event.data)
            .containsExactly("b", "c");
    }

    @Test void availableEventsShouldReturnEmptyWhenAllSeen() {
        ArchivedEventStream<String> stream = new ArchivedEventStream<>(10);
        stream.publish("x"); // id=1
        stream.publish("y"); // id=2

        assertThat(stream.availableEvents(2)).isEmpty();
    }

    @Test void nextEventsShouldBeResolvedImmediatelyWhenEventsAlreadyPresent() {
        ArchivedEventStream<String> stream = new ArchivedEventStream<>(10);
        stream.publish("a"); // id=1

        CompletableFuture<List<IndexedEvent<String>>> p = stream.nextEvents(0);
        assertThat(p).isCompleted();
        assertThat(p.join())
            .singleElement()
            .extracting(event -> event.data)
            .isEqualTo("a");
    }

    @Test void nextEventsShouldNotBeResolvedUntilPublish() {
        ArchivedEventStream<String> stream = new ArchivedEventStream<>(10);

        CompletableFuture<List<IndexedEvent<String>>> p = stream.nextEvents(0);
        assertThat(p).isNotDone();

        stream.publish("a");
        assertThat(p).isCompleted();
        assertThat(p.join())
            .singleElement()
            .extracting(event -> event.data)
            .isEqualTo("a");
    }

    @Test void nextEvents_multipleWaitersAllNotified() {
        ArchivedEventStream<String> stream = new ArchivedEventStream<>(10);

        CompletableFuture<List<IndexedEvent<String>>> p1 = stream.nextEvents(0);
        CompletableFuture<List<IndexedEvent<String>>> p2 = stream.nextEvents(0);
        CompletableFuture<List<IndexedEvent<String>>> p3 = stream.nextEvents(0);

        assertThat(p1).isNotDone();
        assertThat(p2).isNotDone();
        assertThat(p3).isNotDone();

        stream.publish("a");

        Stream.of(p1, p2, p3).forEach(p -> {
            assertThat(p).isCompleted();
            assertThat(p.join())
                .singleElement()
                .extracting(event -> event.data)
                .isEqualTo("a");
        });
    }

    @Test void nextEvents_onlyEventsAfterLastEventSeenAreReturned() throws InterruptedException, ExecutionException {
        ArchivedEventStream<String> stream = new ArchivedEventStream<>(10);
        stream.publish("a"); // id=1
        stream.publish("b"); // id=2

        // Ask for events after id=1 – should get only "b"
        CompletableFuture<List<IndexedEvent<String>>> p = stream.nextEvents(1);
        assertThat(p.isDone());
        assertThat(p.get()).hasSize(1);
        assertThat(p.get().get(0).data).isEqualTo("b");
    }

    @Test void nextEvents_waiterRemovedAfterTrigger() throws InterruptedException, ExecutionException {
        ArchivedEventStream<String> stream = new ArchivedEventStream<>(10);

        CompletableFuture<List<IndexedEvent<String>>> p = stream.nextEvents(0);
        stream.publish("first");
        assertTrue(p.isDone());

        // Publish another event – should not re-trigger the same promise
        // and the archive should contain both events
        stream.publish("second");
        assertThat(stream.archive()).containsExactly("first", "second");
    }

    // -------------------------------------------------------------------------
    // publish() – archive size / dropping oldest
    // -------------------------------------------------------------------------

    @Test void publish_dropsOldestEventWhenArchiveFull() {
        ArchivedEventStream<String> stream = new ArchivedEventStream<>(2);
        stream.publish("a");
        stream.publish("b");
        // This publish should drop "a"
        stream.publish("c");

        List<String> archive = stream.archive();
        assertThat(archive).containsExactly("b", "c");
    }

    // -------------------------------------------------------------------------
    // eventStream() – piping
    // -------------------------------------------------------------------------

    @Test void eventStream_replaysExistingEventsIntoNewStream() throws InterruptedException, ExecutionException {
        ArchivedEventStream<String> stream = new ArchivedEventStream<>(10);
        stream.publish("x"); // id=1
        stream.publish("y"); // id=2

        EventStream<String> es = stream.eventStream();

        // The event stream has buffered events; nextEvent() should be done immediately
        CompletableFuture<String> p = es.nextEvent();
        assertTrue(p.isDone());
        assertThat(p.get()).isEqualTo("x");
    }

    @Test void eventStream_receivesNewPublishesAfterCreation() throws InterruptedException, ExecutionException {
        ArchivedEventStream<String> stream = new ArchivedEventStream<>(10);
        EventStream<String> es = stream.eventStream();

        // Before any publish the nextEvent promise is pending
        CompletableFuture<String> p = es.nextEvent();
        assertFalse(p.isDone());

        stream.publish("new");
        assertTrue(p.isDone());
        assertThat(p.get()).isEqualTo("new");
    }

    @Test void eventStream_multipleStreamsPipedIndependently() throws InterruptedException, ExecutionException {
        ArchivedEventStream<String> stream = new ArchivedEventStream<>(10);

        EventStream<String> es1 = stream.eventStream();
        EventStream<String> es2 = stream.eventStream();

        CompletableFuture<String> p1 = es1.nextEvent();
        CompletableFuture<String> p2 = es2.nextEvent();

        stream.publish("hello");

        assertTrue(p1.isDone());
        assertTrue(p2.isDone());
        assertThat(p1.get()).isEqualTo("hello");
        assertThat(p2.get()).isEqualTo("hello");
    }

    // -------------------------------------------------------------------------
    // IndexedEvent
    // -------------------------------------------------------------------------

    @Test void indexedEvent_idIsSequential() {
        IndexedEvent<String> e1 = new IndexedEvent<>("a");
        IndexedEvent<String> e2 = new IndexedEvent<>("b");
        assertThat(e2.id).isEqualTo(e1.id + 1);
    }

    @Test void indexedEvent_toString() {
        IndexedEvent<String> e = new IndexedEvent<>("data");
        String s = e.toString();
        assertThat(s).startsWith("Event(id:");
        assertThat(s).contains("data");
    }

    @Test void indexedEvent_resetIdGenerator() {
        IndexedEvent<String> e1 = new IndexedEvent<>("a");
        long id1 = e1.id;
        IndexedEvent.resetIdGenerator();
        IndexedEvent<String> e2 = new IndexedEvent<>("b");
        // After reset the id starts at 1 again
        assertThat(e2.id).isEqualTo(1L);
        // Just to suppress unused-variable warning
        assertThat(id1).isGreaterThan(0);
    }

    // -------------------------------------------------------------------------
    // FilterTask (covered via nextEvents/publish)
    // -------------------------------------------------------------------------

    @Test void filterTask_triggerReturnsFalseWhenNoMatchingEvents() {
        // Create a waiter for events after id that hasn't been published yet.
        // Publish an event at a lower id to verify trigger() returns false (no new events).
        ArchivedEventStream<String> stream = new ArchivedEventStream<>(10);
        stream.publish("old"); // id=1

        // Register a waiter that only cares about events with id > current max
        // We capture the promise before any further publishes
        CompletableFuture<List<IndexedEvent<String>>> p = stream.nextEvents(1); // waits for id > 1

        // At this point there are no events with id > 1, so filter is still pending
        assertFalse(p.isDone());

        // Now publish another event which will have id=2 > 1
        stream.publish("new"); // id=2
        assertTrue(p.isDone());
    }

    @Test void nextEvents_publishMultipleEventsBeforeWait() throws InterruptedException, ExecutionException {
        ArchivedEventStream<String> stream = new ArchivedEventStream<>(10);
        stream.publish("a"); // id=1
        stream.publish("b"); // id=2
        stream.publish("c"); // id=3

        // Ask for events seen after id=1
        CompletableFuture<List<IndexedEvent<String>>> p = stream.nextEvents(1);
        assertTrue(p.isDone());

        List<IndexedEvent<String>> events = p.get();
        assertThat(events).hasSize(2);
        assertThat(events.stream().map(e -> e.data)).containsExactly("b", "c");
    }
}
