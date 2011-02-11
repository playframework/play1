package play.libs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import play.exceptions.UnexpectedException;

public class F {

    public static class Promise<V> implements Future<V>, F.Action<V> {

        final CountDownLatch taskLock = new CountDownLatch(1);
        boolean cancelled = false;

        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        public boolean isCancelled() {
            return false;
        }

        public boolean isDone() {
            return invoked;
        }

        public V getOrNull() {
            return result;
        }

        public V get() throws InterruptedException, ExecutionException {
            taskLock.await();
            return result;
        }

        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            taskLock.await(timeout, unit);
            return result;
        }
        List<F.Action<Promise<V>>> callbacks = new ArrayList<F.Action<Promise<V>>>();
        boolean invoked = false;
        V result = null;

        public void invoke(V result) {
            synchronized (this) {
                if (!invoked) {
                    invoked = true;
                    this.result = result;
                    taskLock.countDown();
                } else {
                    return;
                }
            }
            for (F.Action<Promise<V>> callback : callbacks) {
                callback.invoke(this);
            }
        }

        public void onRedeem(F.Action<Promise<V>> callback) {
            synchronized (this) {
                if (!invoked) {
                    callbacks.add(callback);
                }
            }
            if (invoked) {
                callback.invoke(this);
            }
        }

        public static <T> Promise<List<T>> waitAll(final Promise<T>... promises) {
            final CountDownLatch waitAllLock = new CountDownLatch(promises.length);
            final Promise<List<T>> result = new Promise<List<T>>() {

                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    boolean r = true;
                    for (Promise<T> f : promises) {
                        r = r & f.cancel(mayInterruptIfRunning);
                    }
                    return r;
                }

                @Override
                public boolean isCancelled() {
                    boolean r = true;
                    for (Promise<T> f : promises) {
                        r = r & f.isCancelled();
                    }
                    return r;
                }

                @Override
                public boolean isDone() {
                    boolean r = true;
                    for (Promise<T> f : promises) {
                        r = r & f.isDone();
                    }
                    return r;
                }

                @Override
                public List<T> get() throws InterruptedException, ExecutionException {
                    waitAllLock.await();
                    List<T> r = new ArrayList<T>();
                    for (Promise<T> f : promises) {
                        r.add(f.get());
                    }
                    return r;
                }

                @Override
                public List<T> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    waitAllLock.await(timeout, unit);
                    return get();
                }
            };
            final F.Action<Promise<T>> action = new F.Action<Promise<T>>() {

                public void invoke(Promise<T> completed) {
                    waitAllLock.countDown();
                    if (waitAllLock.getCount() == 0) {
                        try {
                            result.invoke(result.get());
                        } catch (Exception e) {
                            throw new UnexpectedException(e);
                        }
                    }
                }
            };
            for (Promise<T> f : promises) {
                f.onRedeem(action);
            }
            return result;
        }

        public static <A, B> Promise<F.Tuple<A, B>> wait2(Promise<A> tA, Promise<B> tB) {
            final Promise<F.Tuple<A, B>> result = new Promise<F.Tuple<A, B>>();
            final Promise<List<Object>> t = waitAll(new Promise[]{tA, tB});
            t.onRedeem(new F.Action<Promise<List<Object>>>() {

                public void invoke(Promise<List<Object>> completed) {
                    List<Object> values = completed.getOrNull();
                    result.invoke(new F.Tuple((A) values.get(0), (B) values.get(1)));
                }
            });
            return result;
        }

        public static <A, B, C> Promise<F.Tuple3<A, B, C>> wait3(Promise<A> tA, Promise<B> tB, Promise<C> tC) {
            final Promise<F.Tuple3<A, B, C>> result = new Promise<F.Tuple3<A, B, C>>();
            final Promise<List<Object>> t = waitAll(new Promise[]{tA, tB, tC});
            t.onRedeem(new F.Action<Promise<List<Object>>>() {

                public void invoke(Promise<List<Object>> completed) {
                    List<Object> values = completed.getOrNull();
                    result.invoke(new F.Tuple3((A) values.get(0), (B) values.get(1), (C) values.get(2)));
                }
            });
            return result;
        }

        private static Promise<F.Tuple<Integer, Promise<Object>>> waitEitherInternal(final Promise<?>... futures) {
            final Promise<F.Tuple<Integer, Promise<Object>>> result = new Promise<F.Tuple<Integer, Promise<Object>>>();
            for (int i = 0; i < futures.length; i++) {
                final int index = i + 1;
                ((Promise<Object>) futures[i]).onRedeem(new F.Action<Promise<Object>>() {

                    public void invoke(Promise<Object> completed) {
                        result.invoke(new F.Tuple(index, completed));
                    }
                });
            }
            return result;
        }

        public static <A, B> Promise<F.Either<A, B>> waitEither(final Promise<A> tA, final Promise<B> tB) {
            final Promise<F.Either<A, B>> result = new Promise<F.Either<A, B>>();
            final Promise<F.Tuple<Integer, Promise<Object>>> t = waitEitherInternal(tA, tB);

            t.onRedeem(new F.Action<Promise<F.Tuple<Integer, Promise<Object>>>>() {

                public void invoke(Promise<F.Tuple<Integer, Promise<Object>>> completed) {
                    F.Tuple<Integer, Promise<Object>> value = completed.getOrNull();
                    switch (value._1) {
                        case 1:
                            result.invoke(F.Either.<A, B>_1((A) value._2.getOrNull()));
                            break;
                        case 2:
                            result.invoke(F.Either.<A, B>_2((B) value._2.getOrNull()));
                            break;
                    }

                }
            });

            return result;
        }

        public static <A, B, C> Promise<F.Either3<A, B, C>> waitEither(final Promise<A> tA, final Promise<B> tB, final Promise<C> tC) {
            final Promise<F.Either3<A, B, C>> result = new Promise<F.Either3<A, B, C>>();
            final Promise<F.Tuple<Integer, Promise<Object>>> t = waitEitherInternal(tA, tB, tC);

            t.onRedeem(new F.Action<Promise<F.Tuple<Integer, Promise<Object>>>>() {

                public void invoke(Promise<F.Tuple<Integer, Promise<Object>>> completed) {
                    F.Tuple<Integer, Promise<Object>> value = completed.getOrNull();
                    switch (value._1) {
                        case 1:
                            result.invoke(F.Either3.<A, B, C>_1((A) value._2.getOrNull()));
                            break;
                        case 2:
                            result.invoke(F.Either3.<A, B, C>_2((B) value._2.getOrNull()));
                            break;
                        case 3:
                            result.invoke(F.Either3.<A, B, C>_3((C) value._2.getOrNull()));
                            break;
                    }

                }
            });

            return result;
        }

        public static <T> Promise<T> waitAny(final Promise<T>... futures) {
            final Promise<T> result = new Promise<T>();

            final F.Action<Promise<T>> action = new F.Action<Promise<T>>() {

                public void invoke(Promise<T> completed) {
                    synchronized (this) {
                        if (result.isDone()) {
                            return;
                        }
                    }
                    result.invoke(completed.getOrNull());
                }
            };

            for (Promise<T> f : futures) {
                f.onRedeem(action);
            }

            return result;
        }
    }

    public static class Timeout extends Promise<Timeout> {

        static Timer timer = new Timer("F.Timeout", true);
        final public String token;

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
            this.token = token;
            final Timeout timeout = this;
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    timeout.invoke(timeout);
                }
            }, delay);
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

        final int bufferSize;
        final ConcurrentLinkedQueue<T> events = new ConcurrentLinkedQueue<T>();
        final List<Promise<T>> waiting = Collections.synchronizedList(new ArrayList<Promise<T>>());

        public EventStream() {
            this.bufferSize = 100;
        }

        public EventStream(int maxBufferSize) {
            this.bufferSize = maxBufferSize;
        }

        public synchronized Promise<T> nextEvent() {
            if (events.isEmpty()) {
                LazyTask task = new LazyTask();
                waiting.add(task);
                return task;
            }
            return new LazyTask(events.peek());
        }

        public synchronized void publish(T event) {
            if (events.size() > bufferSize) {
                events.poll();
            }
            events.offer(event);
            notifyNewEvent();
        }

        void notifyNewEvent() {
            for (Promise<T> task : waiting) {
                task.invoke(events.peek());
            }
            waiting.clear();
        }

        class LazyTask extends Promise<T> {

            public LazyTask() {
            }

            public LazyTask(T value) {
                invoke(value);
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                T value = super.get();
                markAsRead(value);
                return value;
            }

            @Override
            public T getOrNull() {
                T value = super.getOrNull();
                markAsRead(value);
                return value;
            }

            void markAsRead(T value) {
                if (value != null) {
                    events.remove(value);
                }
            }
        }
    }

    public static class UniqueEvent<M> {

        private static final AtomicLong idGenerator = new AtomicLong(1);
        final public M data;
        final public Long id;

        public UniqueEvent(M data) {
            this.data = data;
            this.id = idGenerator.getAndIncrement();
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

        final int archiveSize;
        final ConcurrentLinkedQueue<UniqueEvent<T>> events = new ConcurrentLinkedQueue<UniqueEvent<T>>();
        final List<FilterTask<T>> waiting = Collections.synchronizedList(new ArrayList<FilterTask<T>>());
        final List<EventStream<T>> pipedStreams = new ArrayList<EventStream<T>>();

        public ArchivedEventStream(int archiveSize) {
            this.archiveSize = archiveSize;
        }

        public synchronized EventStream<T> eventStream() {
            final EventStream<T> stream = new EventStream<T>(archiveSize);
            for (UniqueEvent<T> event : events) {
                stream.publish(event.data);
            }
            pipedStreams.add(stream);
            return stream;
        }

        public synchronized Promise<List<UniqueEvent<T>>> nextEvents(long lastEventSeen) {
            FilterTask<T> filter = new FilterTask<T>(lastEventSeen);
            waiting.add(filter);
            notifyNewEvent();
            return filter;
        }

        public synchronized List<UniqueEvent> availableEvents(long lastEventSeen) {
            List<UniqueEvent> result = new ArrayList<UniqueEvent>();
            for (UniqueEvent event : events) {
                if (event.id > lastEventSeen) {
                    result.add(event);
                }
            }
            return result;
        }

        public List<T> archive() {
            List<T> result = new ArrayList<T>();
            for (UniqueEvent<T> event : events) {
                result.add(event.data);
            }
            return result;
        }

        public synchronized void publish(T event) {
            if (events.size() >= archiveSize) {
                events.poll();
            }
            events.offer(new UniqueEvent(event));
            notifyNewEvent();
            for (EventStream<T> eventStream : pipedStreams) {
                eventStream.publish(event);
            }
        }

        void notifyNewEvent() {
            for (ListIterator<FilterTask<T>> it = waiting.listIterator(); it.hasNext();) {
                FilterTask<T> filter = it.next();
                for (UniqueEvent<T> event : events) {
                    filter.propose(event);
                }
                if (filter.trigger()) {
                    it.remove();
                    break;
                }
            }
        }

        static class FilterTask<K> extends Promise<List<UniqueEvent<K>>> {

            final Long lastEventSeen;
            final List<UniqueEvent<K>> newEvents = new ArrayList<UniqueEvent<K>>();

            public FilterTask(Long lastEventSeen) {
                this.lastEventSeen = lastEventSeen;
            }

            public void propose(UniqueEvent<K> event) {
                if (event.id > lastEventSeen) {
                    newEvents.add(event);
                }
            }

            public boolean trigger() {
                if (newEvents.isEmpty()) {
                    return false;
                }
                invoke(newEvents);
                return true;
            }
        }
    }

    public static interface Action0 {

        void invoke();
    }

    public static interface Action<T> {

        void invoke(T result);
    }

    public static abstract class Option<T> implements Iterable<T> {

        public abstract boolean isDefined();

        public abstract T get();

        public static <T> None<T> None() {
            return (None<T>) (Object) None;
        }

        public static <T> Some<T> Some(T value) {
            return new Some<T>(value);
        }
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

        public Iterator<T> iterator() {
            return Collections.<T>emptyList().iterator();
        }

        @Override
        public String toString() {
            return "None";
        }
    }
    public static None<Object> None = new None<Object>();

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

        public Iterator<T> iterator() {
            return Collections.singletonList(value).iterator();
        }

        @Override
        public String toString() {
            return "Some(" + value + ")";
        }
    }

    public static class Either<A, B> {

        final public Option<A> _1;
        final public Option<B> _2;

        private Either(Option<A> _1, Option<B> _2) {
            this._1 = _1;
            this._2 = _2;
        }

        public static <A, B> Either<A, B> _1(A value) {
            return new Either(Option.Some(value), Option.None());
        }

        public static <A, B> Either<A, B> _2(B value) {
            return new Either(Option.None(), Option.Some(value));
        }

        @Override
        public String toString() {
            return "Either(_1: " + _1 + ", _2: " + _2 + ")";
        }
    }

    public static class Either3<A, B, C> {

        final public Option<A> _1;
        final public Option<B> _2;
        final public Option<C> _3;

        private Either3(Option<A> _1, Option<B> _2, Option<C> _3) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
        }

        public static <A, B, C> Either3<A, B, C> _1(A value) {
            return new Either3(Option.Some(value), Option.None(), Option.None());
        }

        public static <A, B, C> Either3<A, B, C> _2(B value) {
            return new Either3(Option.None(), Option.Some(value), Option.None());
        }

        public static <A, B, C> Either3<A, B, C> _3(C value) {
            return new Either3(Option.None(), Option.None(), Option.Some(value));
        }

        @Override
        public String toString() {
            return "Either3(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ")";
        }
    }

    public static class Tuple<A, B> {

        final public A _1;
        final public B _2;

        public Tuple(A _1, B _2) {
            this._1 = _1;
            this._2 = _2;
        }

        @Override
        public String toString() {
            return "Tuple(_1: " + _1 + ", _2: " + _2 + ")";
        }
    }

    public static class Tuple3<A, B, C> {

        final public A _1;
        final public B _2;
        final public C _3;

        public Tuple3(A _1, B _2, C _3) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
        }

        @Override
        public String toString() {
            return "Tuple3(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ")";
        }
    }

    public static abstract class Matcher<T, R> {

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
