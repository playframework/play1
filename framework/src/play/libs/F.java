package play.libs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jboss.netty.channel.ChannelHandlerContext;

import play.Logger;

public class F {


    /**
     * A Function with no arguments.
     */
    @FunctionalInterface
    public interface Function0<R> {
        R apply() throws Throwable;
    }

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
         * @deprecated use {@link #whenComplete(BiConsumer)} instead.
         */
        @Deprecated(since = "1.12", forRemoval = true)
        public void onRedeem(F.Action<Promise<V>> callback) {
            whenComplete((r, e) -> callback.accept(this));
        }

        public static <T> CompletableFuture<List<T>> waitAll(CompletableFuture<T>... promises) {
            Promise<List<T>> result = new Promise<>();

            CompletableFuture.allOf(promises).whenComplete((__, exception) -> {
                if (exception != null) {
                    result.completeExceptionally(exception);
                } else {
                    result.complete(
                        Stream.of(promises)
                            .map(CompletableFuture::join)
                            .toList()
                    );
                }
            });

            return result;
        }

        public static <T> CompletableFuture<List<T>> waitAll(final Collection<? extends CompletableFuture<T>> promises) {
            return waitAll(promises.toArray(Promise[]::new));
        }

        public static <A, B> CompletableFuture<F.Tuple<A, B>> wait2(CompletableFuture<A> tA, CompletableFuture<B> tB) {
            final Promise<F.Tuple<A, B>> result = new Promise<>();

            CompletableFuture.allOf(tA, tB).whenComplete((__, exception) -> {
                if (exception != null) {
                    result.completeExceptionally(exception);
                } else {
                    result.complete(new Tuple<>(tA.join(), tB.join()));
                }
            });

            return result;
        }

        public static <A, B, C> CompletableFuture<F.T3<A, B, C>> wait3(CompletableFuture<A> tA, CompletableFuture<B> tB, CompletableFuture<C> tC) {
            final Promise<F.T3<A, B, C>> result = new Promise<>();

            CompletableFuture.allOf(tA, tB, tC).whenComplete((__, exception) -> {
                if (exception != null) {
                    result.completeExceptionally(exception);
                } else {
                    result.complete(new T3<>(tA.join(), tB.join(), tC.join()));
                }
            });

            return result;
        }

        public static <A, B, C, D> CompletableFuture<F.T4<A, B, C, D>> wait4(CompletableFuture<A> tA, CompletableFuture<B> tB, CompletableFuture<C> tC, CompletableFuture<D> tD) {
            final Promise<F.T4<A, B, C, D>> result = new Promise<>();

            CompletableFuture.allOf(tA, tB, tC, tD).whenComplete((__, exception) -> {
                if (exception != null) {
                    result.completeExceptionally(exception);
                } else {
                    result.complete(new T4<>(tA.join(), tB.join(), tC.join(), tD.join()));
                }
            });

            return result;
        }

        public static <A, B, C, D, E> CompletableFuture<F.T5<A, B, C, D, E>> wait5(CompletableFuture<A> tA, CompletableFuture<B> tB, CompletableFuture<C> tC, CompletableFuture<D> tD, CompletableFuture<E> tE) {
            final Promise<F.T5<A, B, C, D, E>> result = new Promise<>();

            CompletableFuture.allOf(tA, tB, tC, tD, tE).whenComplete((__, exception) -> {
                if (exception != null) {
                    result.completeExceptionally(exception);
                } else {
                    result.complete(new T5<>(tA.join(), tB.join(), tC.join(), tD.join(), tE.join()));
                }
            });

            return result;
        }

        public static <A, B> CompletableFuture<F.Either<A, B>> waitEither(CompletableFuture<A> tA, CompletableFuture<B> tB) {
            final Promise<F.Either<A, B>> result = new Promise<>();

            CompletableFuture.anyOf(
                tA.thenApply(F.Either::<A, B>_1),
                tB.thenApply(F.Either::<A, B>_2)
            ).whenComplete((value, exception) -> {
                if (exception != null) {
                    result.completeExceptionally(exception);
                } else {
                    result.complete((F.Either<A, B>) value);
                }
            });

            return result;
        }

        public static <A, B, C> CompletableFuture<F.E3<A, B, C>> waitEither(CompletableFuture<A> tA, CompletableFuture<B> tB, CompletableFuture<C> tC) {
            final Promise<F.E3<A, B, C>> result = new Promise<>();

            CompletableFuture.anyOf(
                tA.thenApply(F.E3::<A, B, C>_1),
                tB.thenApply(F.E3::<A, B, C>_2),
                tC.thenApply(F.E3::<A, B, C>_3)
            ).whenComplete((value, exception) -> {
                if (exception != null) {
                    result.completeExceptionally(exception);
                } else {
                    result.complete((F.E3<A, B, C>) value);
                }
            });

            return result;
        }

        public static <A, B, C, D> CompletableFuture<F.E4<A, B, C, D>> waitEither(CompletableFuture<A> tA, CompletableFuture<B> tB, CompletableFuture<C> tC, CompletableFuture<D> tD) {
            final Promise<F.E4<A, B, C, D>> result = new Promise<>();

            CompletableFuture.anyOf(
                tA.thenApply(F.E4::<A, B, C, D>_1),
                tB.thenApply(F.E4::<A, B, C, D>_2),
                tC.thenApply(F.E4::<A, B, C, D>_3),
                tD.thenApply(F.E4::<A, B, C, D>_4)
            ).whenComplete((value, exception) -> {
                if (exception != null) {
                    result.completeExceptionally(exception);
                } else {
                    result.complete((F.E4<A, B, C, D>) value);
                }
            });

            return result;
        }

        public static <A, B, C, D, E> CompletableFuture<F.E5<A, B, C, D, E>> waitEither(CompletableFuture<A> tA, CompletableFuture<B> tB, CompletableFuture<C> tC, CompletableFuture<D> tD, CompletableFuture<E> tE) {
            final Promise<F.E5<A, B, C, D, E>> result = new Promise<>();

            CompletableFuture.anyOf(
                tA.thenApply(F.E5::<A, B, C, D, E>_1),
                tB.thenApply(F.E5::<A, B, C, D, E>_2),
                tC.thenApply(F.E5::<A, B, C, D, E>_3),
                tD.thenApply(F.E5::<A, B, C, D, E>_4),
                tE.thenApply(F.E5::<A, B, C, D, E>_5)
            ).whenComplete((value, exception) -> {
                if (exception != null) {
                    result.completeExceptionally(exception);
                } else {
                    result.complete((F.E5<A, B, C, D, E>) value);
                }
            });

            return result;
        }

        public static <T> CompletableFuture<T> waitAny(CompletableFuture<T>... promises) {
            final Promise<T> result = new Promise<>();

            CompletableFuture.anyOf(promises).whenComplete((value, exception) -> {
                if (exception != null) {
                    result.completeExceptionally(exception);
                } else {
                    result.complete((T) value);
                }
            });

            return result;
        }
    }

    public static class Timeout extends Promise<Timeout> {

        static Timer timer = new Timer("F.Timeout", true);
        public final String token;
        public final long delay;

        public Timeout(String delay) {
            this(Time.parseDuration(delay) * 1000);
        }

        public Timeout(String token, String delay) {
            this(token, Time.parseDuration(delay) * 1000);
        }

        public Timeout(long delay) {
            this("timeout", delay);
        }

        public Timeout(String token, long delay) {
            this.delay = delay;
            this.token = token;
            final Timeout timeout = this;
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    timeout.invoke(timeout);
                }
            }, delay);
        }

        @Override
        public String toString() {
            return "Timeout(" + delay + ")";
        }

    }

    public static Timeout Timeout(String delay) {
        return new Timeout(delay);
    }

    public static Timeout Timeout(String token, String delay) {
        return new Timeout(token, delay);
    }

    public static Timeout Timeout(long delay) {
        return new Timeout(delay);
    }

    public static Timeout Timeout(String token, long delay) {
        return new Timeout(token, delay);
    }

    public static class EventStream<T> {

        private final Lock lock = new ReentrantLock();
        private final Queue<T> events = new ConcurrentLinkedQueue<>();
        private final List<Promise<T>> waiting = new ArrayList<>();

        private final int bufferSize;

        public EventStream() {
            this(100);
        }

        public EventStream(int maxBufferSize) {
            this.bufferSize = maxBufferSize;
        }

        public Promise<T> nextEvent() {
            this.lock.lock();
            try {
                var it = this.waiting.iterator();
                if (it.hasNext()) {
                    var wait = it.next();
                    if (wait.isDone()) {
                        it.remove();
                        return wait;
                    }
                }

                var task = new Promise<T>();
                var event = this.events.poll();
                if (event == null) {
                    this.waiting.add(task);
                } else {
                    task.complete(event);
                }

                return task;
            } finally {
                this.lock.unlock();
            }
        }

        public void publish(T event) {
            this.lock.lock();
            try {
                boolean saveEvent = true;
                for (var f : this.waiting) {
                    saveEvent &= !f.complete(event);
                }

                if (saveEvent) {
                    if (this.events.size() > this.bufferSize) {
                        Logger.warn("Dropping message. If this is catastrophic to your app, use a BlockingEvenStream instead");
                        this.events.poll();
                    }
                    this.events.offer(event);
                }
            } finally {
                this.lock.unlock();
            }
        }

    }

    public static class BlockingEventStream<T> {

        private final Lock lock = new ReentrantLock();
        private final List<Promise<T>> waiting = new ArrayList<>();

        private final BlockingQueue<T> events;
        private final ChannelHandlerContext ctx;

        public BlockingEventStream(ChannelHandlerContext ctx) {
            this(100, ctx);
        }

        public BlockingEventStream(int maxBufferSize, ChannelHandlerContext ctx) {
            this.ctx = ctx;
            this.events = new LinkedBlockingQueue<>(maxBufferSize + 10);
        }

        public Promise<T> nextEvent() {
            this.lock.lock();
            try {
                var it = this.waiting.iterator();
                if (it.hasNext()) {
                    var wait = it.next();
                    if (wait.isDone()) {
                        it.remove();
                        return wait;
                    }
                }

                var task = new Promise<T>();
                var event = this.events.poll();
                if (event == null) {
                    task.whenComplete((v, e) -> {
                        //Don't start back up until we get down to half the total capacity to prevent jittering:
                        if (this.events.remainingCapacity() > this.events.size()) {
                            this.ctx.getChannel().setReadable(true);
                        }
                    });
                    this.waiting.add(task);
                } else {
                    task.complete(event);
                }

                return task;
            } finally {
                this.lock.unlock();
            }
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
            this.lock.lock();
            try {
                boolean saveEvent = true;
                for (var f : this.waiting) {
                    saveEvent &= !f.complete(event);
                }

                if (saveEvent) {
                    // This method blocks if the queue is full(read publish method documentation just above)
                    if (this.events.remainingCapacity() == 10) {
                        Logger.trace("events queue is full! Setting readable to false.");
                        this.ctx.getChannel().setReadable(false);
                    }
                    try {
                        this.events.put(event);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            } finally {
                this.lock.unlock();
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

        public Promise<List<IndexedEvent<T>>> nextEvents(long lastEventSeen) {
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
                List<IndexedEvent<T>> result = new ArrayList<>();
                for (IndexedEvent<T> event : this.events) {
                    if (event.id > lastEventSeen) {
                        result.add(event);
                    }
                }
                return result;
            } finally {
                this.lock.unlock();
            }
        }

        public List<T> archive() {
            List<T> result = new ArrayList<>();
            for (IndexedEvent<T> event : this.events) {
                result.add(event.data);
            }
            return result;
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

        void notifyNewEvent() {
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

        private static class FilterTask<K> extends Promise<List<IndexedEvent<K>>> {

            private final List<IndexedEvent<K>> newEvents = new ArrayList<>();
            private final long lastEventSeen;

            FilterTask(long lastEventSeen) {
                this.lastEventSeen = lastEventSeen;
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

    public abstract static class Option<T> implements Iterable<T> {

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

    public static class None<T> extends Option<T> {

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

    public static class Some<T> extends Option<T> {

        final T value;

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
            final Matcher<T, R> firstMatcher = this;
            return new Matcher<T, NR>() {

                @Override
                public Option<NR> match(T o) {
                    for (R r : firstMatcher.match(o)) {
                        return nextMatcher.match(r);
                    }
                    return Option.None();
                }
            };
        }
        public static Matcher<Object, String> String = new Matcher<Object, String>() {

            @Override
            public Option<String> match(Object o) {
                if (o instanceof String) {
                    return Option.Some((String) o);
                }
                return Option.None();
            }
        };

        public static <K> Matcher<Object, K> ClassOf(final Class<K> clazz) {
            return new Matcher<Object, K>() {

                @Override
                public Option<K> match(Object o) {
                    if (o instanceof Option && ((Option) o).isDefined()) {
                        o = ((Option) o).get();
                    }
                    if (clazz.isInstance(o)) {
                        return Option.Some((K) o);
                    }
                    return Option.None();
                }
            };
        }

        public static Matcher<String, String> StartsWith(final String prefix) {
            return new Matcher<String, String>() {

                @Override
                public Option<String> match(String o) {
                    if (o.startsWith(prefix)) {
                        return Option.Some(o);
                    }
                    return Option.None();
                }
            };
        }

        public static Matcher<String, String> Re(final String pattern) {
            return new Matcher<String, String>() {

                @Override
                public Option<String> match(String o) {
                    if (o.matches(pattern)) {
                        return Option.Some(o);
                    }
                    return Option.None();
                }
            };
        }

        public static <X> Matcher<X, X> Equals(final X other) {
            return new Matcher<X, X>() {

                @Override
                public Option<X> match(X o) {
                    if (o.equals(other)) {
                        return Option.Some(o);
                    }
                    return Option.None();
                }
            };
        }
    }
}
