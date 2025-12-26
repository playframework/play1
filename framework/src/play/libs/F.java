package play.libs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.netty.channel.ChannelHandlerContext;

import play.Logger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class F {


    /**
     * A Function with no arguments.
     */
    @FunctionalInterface
    public interface Function0<R> {
        R apply() throws Throwable;
    }

    /**
     * Async result. The public class is retained only as an internal implementation of
     * {@link CompletableFuture} during the migration period; the framework’s public
     * APIs work directly with {@link CompletableFuture}.
     *
     * @deprecated use {@link CompletableFuture} instead.
     */
    @Deprecated(since = "1.12", forRemoval = true)
    public static class Promise<V> extends CompletableFuture<V> implements F.Action<V> {

        @Override
        public <U> Promise<U> newIncompleteFuture() {
            return new Promise<>();
        }

        public V getOrNull() {
            return isDone() && !isCompletedExceptionally() ? join() : null;
        }

        @Override
        public void invoke(V result) {
            complete(result);
        }

        /**
         * @deprecated use {@link #completeExceptionally(Throwable)} instead.
         */
        @Deprecated(since = "1.12", forRemoval = true)
        public void invokeWithException(Throwable t) {
            completeExceptionally(t);
        }

        /**
         * Registers an action that is to be invoked after this promise is invoked.
         * You may register more than one "onRedeem" callback.
         * Each registered callback is guaranteed to be invoked exactly once after
         * this promise has been invoked.
         *
         * <p>
         * The thread from which the "onRedeem" callback is invoked is not defined.
         * </p>
         *
         * @param callback
         *            The callback action to invoke when this promise.
         *
         * @deprecated use {@link CompletableFuture#whenComplete(java.util.function.BiConsumer)} instead.
         */
        @Deprecated(since = "1.12", forRemoval = true)
        public void onRedeem(F.Action<Promise<V>> callback) {
            whenComplete((r, e) -> callback.accept(this));
        }

        @SafeVarargs
        public static <T> CompletableFuture<List<T>> waitAll(CompletableFuture<T>... promises) {
            return CompletableFuture.allOf(promises).thenApply(
                __ -> Stream.of(promises)
                    .map(CompletableFuture::join)
                    .toList()
            );
        }

        public static <T> CompletableFuture<List<T>> waitAll(final Collection<? extends CompletableFuture<T>> promises) {
            return waitAll(promises.toArray(Promise[]::new));
        }

        public static <A, B> CompletableFuture<F.Tuple<A, B>> wait2(CompletableFuture<A> tA, CompletableFuture<B> tB) {
            return CompletableFuture.allOf(tA, tB)
                .thenApply(__ -> new Tuple<>(tA.join(), tB.join()));
        }

        public static <A, B, C> CompletableFuture<F.T3<A, B, C>> wait3(CompletableFuture<A> tA, CompletableFuture<B> tB, CompletableFuture<C> tC) {
            return CompletableFuture.allOf(tA, tB, tC)
                .thenApply(__ -> new T3<>(tA.join(), tB.join(), tC.join()));
        }

        public static <A, B, C, D> CompletableFuture<F.T4<A, B, C, D>> wait4(CompletableFuture<A> tA, CompletableFuture<B> tB, CompletableFuture<C> tC, CompletableFuture<D> tD) {
            return CompletableFuture.allOf(tA, tB, tC, tD)
                .thenApply(__ -> new T4<>(tA.join(), tB.join(), tC.join(), tD.join()));
        }

        public static <A, B, C, D, E> CompletableFuture<F.T5<A, B, C, D, E>> wait5(CompletableFuture<A> tA, CompletableFuture<B> tB, CompletableFuture<C> tC, CompletableFuture<D> tD, CompletableFuture<E> tE) {
            return CompletableFuture.allOf(tA, tB, tC, tD, tE)
                .thenApply(__ -> new T5<>(tA.join(), tB.join(), tC.join(), tD.join(), tE.join()));
        }

        public static <A, B> CompletableFuture<F.Either<A, B>> waitEither(CompletableFuture<A> tA, CompletableFuture<B> tB) {
            var result = new CompletableFuture<F.Either<A, B>>();
            final AtomicBoolean winnerFound = new AtomicBoolean(false);

            tA.whenComplete((value, exception) -> {
                // the first completion (a value or a failure) wins;
                // joining the winner is the single proxy read that
                // consumes its LazyTask exactly once, losers are never
                // joined so their events stay in the queue untouched
                if (winnerFound.compareAndSet(false, true)) {
                    if (exception == null) {
                        result.complete(E2._1(tA.join()));
                    } else {
                        result.completeExceptionally(exception);
                    }
                    tB.cancel(false);
                }
            });

            tB.whenComplete((value, exception) -> {
                // the first completion (a value or a failure) wins;
                // joining the winner is the single proxy read that
                // consumes its LazyTask exactly once, losers are never
                // joined so their events stay in the queue untouched
                if (winnerFound.compareAndSet(false, true)) {
                    if (exception == null) {
                        result.complete(E2._2(tB.join()));
                    } else {
                        result.completeExceptionally(exception);
                    }
                    tA.cancel(false);
                }
            });

            return result;
        }

        public static <A, B, C> CompletableFuture<F.E3<A, B, C>> waitEither(CompletableFuture<A> tA, CompletableFuture<B> tB, CompletableFuture<C> tC) {
            var result = new CompletableFuture<F.E3<A, B, C>>();
            final AtomicBoolean winnerFound = new AtomicBoolean(false);

            tA.whenComplete((value, exception) -> {
                // the first completion (a value or a failure) wins;
                // joining the winner is the single proxy read that
                // consumes its LazyTask exactly once, losers are never
                // joined so their events stay in the queue untouched
                if (winnerFound.compareAndSet(false, true)) {
                    if (exception == null) {
                        result.complete(E3._1(tA.join()));
                    } else {
                        result.completeExceptionally(exception);
                    }
                    tB.cancel(false);
                    tC.cancel(false);
                }
            });

            tB.whenComplete((value, exception) -> {
                // the first completion (a value or a failure) wins;
                // joining the winner is the single proxy read that
                // consumes its LazyTask exactly once, losers are never
                // joined so their events stay in the queue untouched
                if (winnerFound.compareAndSet(false, true)) {
                    if (exception == null) {
                        result.complete(E3._2(tB.join()));
                    } else {
                        result.completeExceptionally(exception);
                    }
                    tA.cancel(false);
                    tC.cancel(false);
                }
            });

            tC.whenComplete((value, exception) -> {
                // the first completion (a value or a failure) wins;
                // joining the winner is the single proxy read that
                // consumes its LazyTask exactly once, losers are never
                // joined so their events stay in the queue untouched
                if (winnerFound.compareAndSet(false, true)) {
                    if (exception == null) {
                        result.complete(E3._3(tC.join()));
                    } else {
                        result.completeExceptionally(exception);
                    }
                    tA.cancel(false);
                    tB.cancel(false);
                }
            });

            return result;
        }

        public static <A, B, C, D> CompletableFuture<F.E4<A, B, C, D>> waitEither(CompletableFuture<A> tA, CompletableFuture<B> tB, CompletableFuture<C> tC, CompletableFuture<D> tD) {
            var result = new CompletableFuture<F.E4<A, B, C, D>>();
            final AtomicBoolean winnerFound = new AtomicBoolean(false);

            tA.whenComplete((value, exception) -> {
                // the first completion (a value or a failure) wins;
                // joining the winner is the single proxy read that
                // consumes its LazyTask exactly once, losers are never
                // joined so their events stay in the queue untouched
                if (winnerFound.compareAndSet(false, true)) {
                    if (exception == null) {
                        result.complete(E4._1(tA.join()));
                    } else {
                        result.completeExceptionally(exception);
                    }
                    tB.cancel(false);
                    tC.cancel(false);
                    tD.cancel(false);
                }
            });

            tB.whenComplete((value, exception) -> {
                // the first completion (a value or a failure) wins;
                // joining the winner is the single proxy read that
                // consumes its LazyTask exactly once, losers are never
                // joined so their events stay in the queue untouched
                if (winnerFound.compareAndSet(false, true)) {
                    if (exception == null) {
                        result.complete(E4._2(tB.join()));
                    } else {
                        result.completeExceptionally(exception);
                    }
                    tA.cancel(false);
                    tC.cancel(false);
                    tD.cancel(false);
                }
            });

            tC.whenComplete((value, exception) -> {
                // the first completion (a value or a failure) wins;
                // joining the winner is the single proxy read that
                // consumes its LazyTask exactly once, losers are never
                // joined so their events stay in the queue untouched
                if (winnerFound.compareAndSet(false, true)) {
                    if (exception == null) {
                        result.complete(E4._3(tC.join()));
                    } else {
                        result.completeExceptionally(exception);
                    }
                    tA.cancel(false);
                    tB.cancel(false);
                    tD.cancel(false);
                }
            });

            tD.whenComplete((value, exception) -> {
                // the first completion (a value or a failure) wins;
                // joining the winner is the single proxy read that
                // consumes its LazyTask exactly once, losers are never
                // joined so their events stay in the queue untouched
                if (winnerFound.compareAndSet(false, true)) {
                    if (exception == null) {
                        result.complete(E4._4(tD.join()));
                    } else {
                        result.completeExceptionally(exception);
                    }
                    tA.cancel(false);
                    tB.cancel(false);
                    tC.cancel(false);
                }
            });

            return result;
        }

        public static <A, B, C, D, E> CompletableFuture<F.E5<A, B, C, D, E>> waitEither(CompletableFuture<A> tA, CompletableFuture<B> tB, CompletableFuture<C> tC, CompletableFuture<D> tD, CompletableFuture<E> tE) {
            var result = new CompletableFuture<F.E5<A, B, C, D, E>>();
            final AtomicBoolean winnerFound = new AtomicBoolean(false);

            tA.whenComplete((value, exception) -> {
                // the first completion (a value or a failure) wins;
                // joining the winner is the single proxy read that
                // consumes its LazyTask exactly once, losers are never
                // joined so their events stay in the queue untouched
                if (winnerFound.compareAndSet(false, true)) {
                    if (exception == null) {
                        result.complete(E5._1(tA.join()));
                    } else {
                        result.completeExceptionally(exception);
                    }
                    tB.cancel(false);
                    tC.cancel(false);
                    tD.cancel(false);
                    tE.cancel(false);
                }
            });

            tB.whenComplete((value, exception) -> {
                // the first completion (a value or a failure) wins;
                // joining the winner is the single proxy read that
                // consumes its LazyTask exactly once, losers are never
                // joined so their events stay in the queue untouched
                if (winnerFound.compareAndSet(false, true)) {
                    if (exception == null) {
                        result.complete(E5._2(tB.join()));
                    } else {
                        result.completeExceptionally(exception);
                    }
                    tA.cancel(false);
                    tC.cancel(false);
                    tD.cancel(false);
                    tE.cancel(false);
                }
            });

            tC.whenComplete((value, exception) -> {
                // the first completion (a value or a failure) wins;
                // joining the winner is the single proxy read that
                // consumes its LazyTask exactly once, losers are never
                // joined so their events stay in the queue untouched
                if (winnerFound.compareAndSet(false, true)) {
                    if (exception == null) {
                        result.complete(E5._3(tC.join()));
                    } else {
                        result.completeExceptionally(exception);
                    }
                    tA.cancel(false);
                    tB.cancel(false);
                    tD.cancel(false);
                    tE.cancel(false);
                }
            });

            tD.whenComplete((value, exception) -> {
                // the first completion (a value or a failure) wins;
                // joining the winner is the single proxy read that
                // consumes its LazyTask exactly once, losers are never
                // joined so their events stay in the queue untouched
                if (winnerFound.compareAndSet(false, true)) {
                    if (exception == null) {
                        result.complete(E5._4(tD.join()));
                    } else {
                        result.completeExceptionally(exception);
                    }
                    tA.cancel(false);
                    tB.cancel(false);
                    tC.cancel(false);
                    tE.cancel(false);
                }
            });

            tE.whenComplete((value, exception) -> {
                // the first completion (a value or a failure) wins;
                // joining the winner is the single proxy read that
                // consumes its LazyTask exactly once, losers are never
                // joined so their events stay in the queue untouched
                if (winnerFound.compareAndSet(false, true)) {
                    if (exception == null) {
                        result.complete(E5._5(tE.join()));
                    } else {
                        result.completeExceptionally(exception);
                    }
                    tA.cancel(false);
                    tB.cancel(false);
                    tC.cancel(false);
                    tD.cancel(false);
                }
            });

            return result;
        }

        public static <T> CompletableFuture<T> waitAny(CompletableFuture<T>... promises) {
            final CompletableFuture<T> result = new CompletableFuture<>();
            final AtomicBoolean winnerFound = new AtomicBoolean(false);

            for (final CompletableFuture<T> promise : promises) {
                promise.whenComplete((value, exception) -> {
                    if (exception != null) {
                        if (!result.isDone()) {
                            result.completeExceptionally(exception);
                        }
                        return;
                    }
                    // First future to complete wins; joining the winner
                    // triggers markAsRead() on its LazyTask so the event is
                    // consumed exactly once. Subsequent completions are ignored.
                    if (winnerFound.compareAndSet(false, true)) {
                        try {
                            result.complete(promise.join());
                        } catch (Throwable e) {
                            result.completeExceptionally(e);
                        }
                    }
                });
            }

            return result;
        }
    }

    /**
     * @deprecated use {@link CompletableFuture#orTimeout(long, TimeUnit)} or
     *             {@link CompletableFuture#completeOnTimeout(Object, long, TimeUnit)} instead
     */
    @Deprecated(since = "1.12", forRemoval = true)
    public static class Timeout extends CompletableFuture<Timeout> {

        public final long delay;

        public Timeout(String delay) {
            this(Time.parseDuration(delay) * 1000L);
        }

        public Timeout(long delay) {
            this(delay, null);
            completeOnTimeout(this, delay, MILLISECONDS);
        }

        private Timeout(long delay, Void unused) {
            this.delay = delay;
        }

        @Override
        public Timeout newIncompleteFuture() {
            return new Timeout(this.delay, null);
        }

        @Override
        public String toString() {
            return "Timeout(" + delay + ")";
        }

    }

    public static Timeout Timeout(String delay) {
        return new Timeout(delay);
    }

    public static Timeout Timeout(long delay) {
        return new Timeout(delay);
    }

    public static class EventStream<T> {

        final Queue<T> events = new ConcurrentLinkedQueue<>();
        final List<CompletableFuture<T>> waiting = new ArrayList<>();

        private final int bufferSize;

        public EventStream() {
            this(100);
        }

        public EventStream(int maxBufferSize) {
            if (maxBufferSize < 0) {
                throw new IllegalArgumentException("maxBufferSize must not be negative");
            }
            this.bufferSize = maxBufferSize;
        }

        public synchronized CompletableFuture<T> nextEvent() {
            var event = events.peek();
            if (event != null) {
                return new LazyTask(event);
            }

            var task = new LazyTask();
            waiting.add(task);
            return task;
        }

        public synchronized void publish(T event) {
            if (events.size() > bufferSize) {
                Logger.warn("Dropping message. If this is catastrophic to your app, use a BlockingEvenStream instead");
                events.poll();
            }
            events.offer(event);
            notifyNewEvent();
        }

        void notifyNewEvent() {
            T value = events.peek();
            // take a snapshot and empty the subscription list first: a
            // handler running synchronously inside complete() may call
            // nextEvent() reentrantly, and such a late registration must
            // survive the completion of the earlier tasks
            var toNotify = new ArrayList<>(waiting);
            waiting.clear();
            for (var task : toNotify) {
                task.complete(value);
            }
        }

        // An event is consumed only when a subscriber actually reads the
        // returned promise through get()/getOrNull()/join(): delivery merely
        // notifies the subscribers, the event itself stays in the queue until
        // one of the delivered promises is read.
        private final class LazyTask extends CompletableFuture<T> {

            private final AtomicBoolean consumed = new AtomicBoolean(false);

            LazyTask() {
            }

            LazyTask(T value) {
                complete(value);
            }

            // Every family member is its own gate on the event: whichever
            // one first reads a value removes it from the queue, and any
            // duplicate removal attempt is a harmless identity no-op, so
            // no consumption state needs to be shared between them.
            @Override
            public LazyTask newIncompleteFuture() {
                return new LazyTask();
            }

            // a single LazyTask consumes exactly one event, once: repeated
            // reads of the same promise (e.g. join() followed by get()) must
            // not remove further events from the queue
            @Override
            public T getNow(T valueIfAbsent) {
                T value = super.getNow(valueIfAbsent);
                // on exceptional completion getNow() returns the default
                // valueIfAbsent, which is not the delivered event
                if (isDone() && !isCompletedExceptionally()) {
                    markAsRead(value);
                }
                return value;
            }

            // remove only the exact delivered instance, by identity: event
            // values may compare equal (e.g. two identical chat messages)
            // and must not be mixed up
            @Override
            public T get() throws InterruptedException, ExecutionException {
                T value = super.get();
                markAsRead(value);
                return value;
            }

            @Override
            public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                T value = super.get(timeout, unit);
                markAsRead(value);
                return value;
            }

            @Override
            public T join() {
                T value = super.join();
                markAsRead(value);
                return value;
            }

            private void markAsRead(T value) {
                if (value != null && consumed.compareAndSet(false, true)) {
                    var it = EventStream.this.events.iterator();
                    while (it.hasNext()) {
                        if (it.next() == value) {
                            it.remove();
                            break;
                        }
                    }
                }
            }
        }

    }

    public static class BlockingEventStream<T> {

        final LinkedBlockingQueue<T> events;
        final List<CompletableFuture<T>> waiting = new ArrayList<>();
        final ChannelHandlerContext ctx;

        public BlockingEventStream(ChannelHandlerContext ctx) {
            this(100, ctx);
        }

        public BlockingEventStream(int maxBufferSize, ChannelHandlerContext ctx) {
            if (maxBufferSize < 0) {
                throw new IllegalArgumentException("maxBufferSize must not be negative");
            }
            this.ctx = ctx;
            this.events = new LinkedBlockingQueue<>(maxBufferSize + 10);
        }

        public synchronized CompletableFuture<T> nextEvent() {
            if (!events.isEmpty()) {
                return new LazyTask(events.peek());
            }

            var task = new LazyTask();
            waiting.add(task);
            return task;
        }

        //NOTE: cannot synchronize since events.put may block when system is overloaded.
        //Normally, I HATE blocking an NIO thread, but to do this correct, we need a token from netty that we can use to disable
        //the socket reads completely(ie. stop reading from socket when queue is full) as in normal NIO operations if you stop reading
        //from the socket, the local nic buffer fills up, then the remote nic buffer fills(the client's nic), and so the client is informed
        //he can't write anymore just yet (or he blocks if he is synchronous).
        //Then when someone pulls from the queue, the token would be set to enabled allowing to read from nic buffer again and it all propagates
        //This is normal flow control with NIO but since it is not done properly, this at least fixes the issue where websocket break down and
        //skip packets.  They no longer skip packets anymore.
        public void publish(T event) {
            try {
                // This method blocks if the queue is full(read publish method documentation just above)
                if (events.remainingCapacity() == 10) {
                    Logger.trace("events queue is full! Setting readable to false.");
                    ctx.channel().config().setAutoRead(false);
                }
                events.put(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            notifyNewEvent();
        }

        synchronized void notifyNewEvent() {
            T value = events.peek();
            // take a snapshot and empty the subscription list first: a
            // handler running synchronously inside complete() may call
            // nextEvent() reentrantly, and such a late registration must
            // survive the completion of the earlier tasks
            var toNotify = new ArrayList<>(waiting);
            waiting.clear();
            for (var task : toNotify) {
                task.complete(value);
            }
        }

        // An event is consumed only when a subscriber actually reads the
        // returned promise through get()/getOrNull()/join(): delivery merely
        // notifies the subscribers, the event itself stays in the queue until
        // one of the delivered promises is read.
        private final class LazyTask extends CompletableFuture<T> {

            private final AtomicBoolean consumed = new AtomicBoolean(false);

            LazyTask() {
            }

            LazyTask(T value) {
                complete(value);
            }

            // Every family member is its own gate on the event: whichever
            // one first reads a value removes it from the queue, and any
            // duplicate removal attempt is a harmless identity no-op, so
            // no consumption state needs to be shared between them.
            @Override
            public LazyTask newIncompleteFuture() {
                return new LazyTask();
            }

            // a single LazyTask consumes exactly one event, once: repeated
            // reads of the same promise (e.g. join() followed by get()) must
            // not remove further events from the queue
            @Override
            public T getNow(T valueIfAbsent) {
                T value = super.getNow(valueIfAbsent);
                // on exceptional completion getNow() returns the default
                // valueIfAbsent, which is not the delivered event
                if (isDone() && !isCompletedExceptionally()) {
                    markAsRead(value);
                }
                return value;
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                T value = super.get();
                markAsRead(value);
                return value;
            }

            @Override
            public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                T value = super.get(timeout, unit);
                markAsRead(value);
                return value;
            }

            @Override
            public T join() {
                T value = super.join();
                markAsRead(value);
                return value;
            }

            // remove only the exact delivered instance, by identity: event
            // values may compare equal (e.g. two identical chat messages)
            // and must not be mixed up
            private void markAsRead(T value) {
                if (value != null && consumed.compareAndSet(false, true)) {
                    var it = BlockingEventStream.this.events.iterator();
                    while (it.hasNext()) {
                        if (it.next() == value) {
                            it.remove();
                            break;
                        }
                    }
                    //Don't start back up until we get down to half the total capacity to prevent jittering:
                    if (BlockingEventStream.this.events.remainingCapacity() > BlockingEventStream.this.events.size()) {
                        BlockingEventStream.this.ctx.channel().config().setAutoRead(true);
                    }
                }
            }
        }

    }

    public static final class IndexedEvent<M> {

        private static final AtomicLong idGenerator = new AtomicLong(1);

        public final Long id = idGenerator.getAndIncrement();
        public final M data;

        public IndexedEvent(M data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return "Event(id: " + id + ", " + data + ")";
        }

        public static void resetIdGenerator() {
            idGenerator.set(1);
        }
    }

    public static class ArchivedEventStream<T> {

        private final Lock lock = new ReentrantLock();
        private final Queue<IndexedEvent<T>> events = new ConcurrentLinkedQueue<>();
        private final List<FilterTask<T>> waiting = new ArrayList<>();
        private final List<EventStream<T>> pipedStreams = new ArrayList<>();

        private final int archiveSize;

        public ArchivedEventStream(int archiveSize) {
            this.archiveSize = archiveSize;
        }

        public EventStream<T> eventStream() {
            this.lock.lock();
            try {
                EventStream<T> stream = new EventStream<>(this.archiveSize);
                for (IndexedEvent<T> event : this.events) {
                    stream.publish(event.data);
                }
                this.pipedStreams.add(stream);
                return stream;
            } finally {
                this.lock.unlock();
            }
        }

        public CompletableFuture<List<IndexedEvent<T>>> nextEvents(long lastEventSeen) {
            this.lock.lock();
            try {
                FilterTask<T> filter = new FilterTask<>(lastEventSeen);
                this.waiting.add(filter);
                notifyNewEvent();
                return filter;
            } finally {
                this.lock.unlock();
            }
        }

        public List<IndexedEvent<T>> availableEvents(long lastEventSeen) {
            this.lock.lock();
            try {
                return this.events.stream()
                    .filter(event -> event.id > lastEventSeen)
                    .toList();
            } finally {
                this.lock.unlock();
            }
        }

        public List<T> archive() {
            return this.events.stream()
                .map(event -> event.data)
                .toList();
        }

        public void publish(T event) {
            this.lock.lock();
            try {
                if (this.events.size() >= this.archiveSize) {
                    Logger.warn("Dropping message.  If this is catastrophic to your app, use a BlockingEvenStream instead");
                    this.events.poll();
                }
                this.events.offer(new IndexedEvent<>(event));
                notifyNewEvent();
                for (EventStream<T> eventStream : this.pipedStreams) {
                    eventStream.publish(event);
                }
            } finally {
                this.lock.unlock();
            }
        }

        private void notifyNewEvent() {
            for (ListIterator<FilterTask<T>> it = waiting.listIterator(); it.hasNext();) {
                FilterTask<T> filter = it.next();
                for (IndexedEvent<T> event : events) {
                    filter.propose(event);
                }
                if (filter.trigger()) {
                    it.remove();
                }
            }
        }

        private static class FilterTask<K> extends CompletableFuture<List<IndexedEvent<K>>> {

            private final List<IndexedEvent<K>> newEvents = new ArrayList<>();
            private final long lastEventSeen;

            FilterTask(long lastEventSeen) {
                this.lastEventSeen = lastEventSeen;
            }

            @Override
            public FilterTask<K> newIncompleteFuture() {
                return new FilterTask<>(this.lastEventSeen);
            }

            void propose(IndexedEvent<K> event) {
                if (event.id > this.lastEventSeen) {
                    this.newEvents.add(event);
                }
            }

            boolean trigger() {
                if (this.newEvents.isEmpty()) {
                    return false;
                }
                complete(this.newEvents);
                return true;
            }
        }
    }

    @FunctionalInterface
    public interface Action0 extends Runnable {

        void invoke();

        @Override
        default void run() {
            invoke();
        }

    }

    @FunctionalInterface
    public interface Action<T> extends Consumer<T> {

        void invoke(T result);

        @Override
        default void accept(T result) {
            invoke(result);
        }

    }

    public static abstract sealed class Option<T> implements Iterable<T> permits F.Some, F.None {

        public abstract boolean isDefined();

        public abstract T get();

        public static <T> None<T> None() {
            return (None<T>) None;
        }

        public static <T> Some<T> Some(T value) {
            return new Some<>(value);
        }
    }

    public static <A> Some<A> Some(A a) {
        return new Some<A>(a);
    }

    public static final class None<T> extends Option<T> {

        private None() {
        }

        @Override
        public boolean isDefined() {
            return false;
        }

        @Override
        public T get() {
            throw new IllegalStateException("No value");
        }

        @Override
        public Iterator<T> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public String toString() {
            return "None";
        }
    }
    public static None<Object> None = new None<>();

    public static final class Some<T> extends Option<T> {

        private final T value;

        public Some(T value) {
            this.value = value;
        }

        @Override
        public boolean isDefined() {
            return true;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public Iterator<T> iterator() {
            return Collections.singletonList(value).iterator();
        }

        @Override
        public String toString() {
            return "Some(" + value + ")";
        }
    }

    public static class Either<A, B> {

        public final Option<A> _1;
        public final Option<B> _2;

        private Either(Option<A> _1, Option<B> _2) {
            this._1 = _1;
            this._2 = _2;
        }

        public static <A, B> Either<A, B> _1(A value) {
            return new Either(Some(value), None);
        }

        public static <A, B> Either<A, B> _2(B value) {
            return new Either(None, Some(value));
        }

        @Override
        public String toString() {
            return "E2(_1: " + _1 + ", _2: " + _2 + ")";
        }
    }

    public static class E2<A, B> extends Either<A, B> {

        private E2(Option<A> _1, Option<B> _2) {
            super(_1, _2);
        }
    }

    public static class E3<A, B, C> {

        public final Option<A> _1;
        public final Option<B> _2;
        public final Option<C> _3;

        private E3(Option<A> _1, Option<B> _2, Option<C> _3) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
        }

        public static <A, B, C> E3<A, B, C> _1(A value) {
            return new E3(Some(value), None, None);
        }

        public static <A, B, C> E3<A, B, C> _2(B value) {
            return new E3(None, Some(value), None);
        }

        public static <A, B, C> E3<A, B, C> _3(C value) {
            return new E3(None, None, Some(value));
        }

        @Override
        public String toString() {
            return "E3(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ")";
        }
    }

    public static class E4<A, B, C, D> {

        public final Option<A> _1;
        public final Option<B> _2;
        public final Option<C> _3;
        public final Option<D> _4;

        private E4(Option<A> _1, Option<B> _2, Option<C> _3, Option<D> _4) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
        }

        public static <A, B, C, D> E4<A, B, C, D> _1(A value) {
            return new E4(Option.Some(value), None, None, None);
        }

        public static <A, B, C, D> E4<A, B, C, D> _2(B value) {
            return new E4(None, Some(value), None, None);
        }

        public static <A, B, C, D> E4<A, B, C, D> _3(C value) {
            return new E4(None, None, Some(value), None);
        }

        public static <A, B, C, D> E4<A, B, C, D> _4(D value) {
            return new E4(None, None, None, Some(value));
        }

        @Override
        public String toString() {
            return "E4(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ", _4:" + _4 + ")";
        }
    }

    public static class E5<A, B, C, D, E> {

        public final Option<A> _1;
        public final Option<B> _2;
        public final Option<C> _3;
        public final Option<D> _4;
        public final Option<E> _5;

        private E5(Option<A> _1, Option<B> _2, Option<C> _3, Option<D> _4, Option<E> _5) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
        }

        public static <A, B, C, D, E> E5<A, B, C, D, E> _1(A value) {
            return new E5(Option.Some(value), None, None, None, None);
        }

        public static <A, B, C, D, E> E5<A, B, C, D, E> _2(B value) {
            return new E5(None, Option.Some(value), None, None, None);
        }

        public static <A, B, C, D, E> E5<A, B, C, D, E> _3(C value) {
            return new E5(None, None, Option.Some(value), None, None);
        }

        public static <A, B, C, D, E> E5<A, B, C, D, E> _4(D value) {
            return new E5(None, None, None, Option.Some(value), None);
        }

        public static <A, B, C, D, E> E5<A, B, C, D, E> _5(E value) {
            return new E5(None, None, None, None, Option.Some(value));
        }

        @Override
        public String toString() {
            return "E5(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ", _4:" + _4 + ", _5:" + _5 + ")";
        }
    }

    public static class Tuple<A, B> {

        public final A _1;
        public final B _2;

        public Tuple(A _1, B _2) {
            this._1 = _1;
            this._2 = _2;
        }

        @Override
        public String toString() {
            return "T2(_1: " + _1 + ", _2: " + _2 + ")";
        }
    }

    public static <A, B> Tuple<A, B> Tuple(A a, B b) {
        return new Tuple(a, b);
    }

    public static class T2<A, B> extends Tuple<A, B> {

        public T2(A _1, B _2) {
            super(_1, _2);
        }
    }

    public static <A, B> T2<A, B> T2(A a, B b) {
        return new T2(a, b);
    }

    public static class T3<A, B, C> {

        public final A _1;
        public final B _2;
        public final C _3;

        public T3(A _1, B _2, C _3) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
        }

        @Override
        public String toString() {
            return "T3(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ")";
        }
    }

    public static <A, B, C> T3<A, B, C> T3(A a, B b, C c) {
        return new T3(a, b, c);
    }

    public static class T4<A, B, C, D> {

        public final A _1;
        public final B _2;
        public final C _3;
        public final D _4;

        public T4(A _1, B _2, C _3, D _4) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
        }

        @Override
        public String toString() {
            return "T4(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ", _4:" + _4 + ")";
        }
    }

    public static <A, B, C, D> T4<A, B, C, D> T4(A a, B b, C c, D d) {
        return new T4<>(a, b, c, d);
    }

    public static class T5<A, B, C, D, E> {

        public final A _1;
        public final B _2;
        public final C _3;
        public final D _4;
        public final E _5;

        public T5(A _1, B _2, C _3, D _4, E _5) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
        }

        @Override
        public String toString() {
            return "T5(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ", _4:" + _4 + ", _5:" + _5 + ")";
        }
    }

    public static <A, B, C, D, E> T5<A, B, C, D, E> T5(A a, B b, C c, D d, E e) {
        return new T5<>(a, b, c, d, e);
    }

    public abstract static class Matcher<T, R> {

        public abstract Option<R> match(T o);

        public Option<R> match(Option<T> o) {
            if (o.isDefined()) {
                return match(o.get());
            }
            return Option.None();
        }

        public <NR> Matcher<T, NR> and(final Matcher<R, NR> nextMatcher) {
            return new Matcher<>() {
                @Override public Option<NR> match(T o) {
                    for (R r : F.Matcher.this.match(o)) {
                        return nextMatcher.match(r);
                    }
                    return Option.None();
                }
            };
        }

        public static final Matcher<Object, String> String = new Matcher<>() {
            @Override public Option<String> match(Object o) {
                if (o instanceof String s) {
                    return Option.Some(s);
                }
                return Option.None();
            }
        };

        public static <K> Matcher<Object, K> ClassOf(final Class<K> clazz) {
            return new Matcher<>() {
                @Override public Option<K> match(Object o) {
                    if (o instanceof Option option && option.isDefined()) {
                        o = option.get();
                    }
                    if (clazz.isInstance(o)) {
                        return Option.Some((K) o);
                    }
                    return Option.None();
                }
            };
        }

        public static Matcher<String, String> StartsWith(final String prefix) {
            return new Matcher<>() {
                @Override public Option<String> match(String o) {
                    if (o.startsWith(prefix)) {
                        return Option.Some(o);
                    }
                    return Option.None();
                }
            };
        }

        public static Matcher<String, String> Re(final String pattern) {
            return new Matcher<>() {
                @Override public Option<String> match(String o) {
                    if (o.matches(pattern)) {
                        return Option.Some(o);
                    }
                    return Option.None();
                }
            };
        }

        public static <X> Matcher<X, X> Equals(final X other) {
            return new Matcher<>() {
                @Override public Option<X> match(X o) {
                    if (o.equals(other)) {
                        return Option.Some(o);
                    }
                    return Option.None();
                }
            };
        }
    }
}
