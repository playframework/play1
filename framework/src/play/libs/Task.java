package play.libs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import play.exceptions.UnexpectedException;

public class Task<V> implements Future<V>, F.Action<V> {

    final CountDownLatch taskLock = new CountDownLatch(1);
    Future<V> innerFuture;

    public boolean cancel(boolean mayInterruptIfRunning) {
        return innerFuture.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
        return innerFuture.isCancelled();
    }

    public boolean isDone() {
        return invoked;
    }

    public V get() throws InterruptedException, ExecutionException {
        taskLock.await();
        return result;
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        taskLock.await(timeout, unit);
        return result;
    }

    // Wrap existing future
    public void wrap(Future<V> innerFuture) {
        this.innerFuture = innerFuture;
    }

    // Callbacks
    List<F.Action<V>> callbacks = new ArrayList<F.Action<V>>();
    boolean invoked = false;
    V result = null;

    public void invoke(V result) {
        synchronized (this) {
            if (!invoked) {
                invoked = true;
                this.result = result;
                taskLock.countDown();
            }
        }
        for (F.Action<V> callback : callbacks) {
            callback.invoke(result);
        }
    }

    public void onCompletion(F.Action<V> callback) {
        synchronized (this) {
            if (!invoked) {
                callbacks.add(callback);
            }
        }
        if (invoked) {
            callback.invoke(result);
        }
    }

    //
    public static <T> Task<List<T>> waitAll(final Task<T>... futures) {
        final CountDownLatch waitAllLock = new CountDownLatch(futures.length);
        final Task<List<T>> result = new Task<List<T>>() {

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean r = true;
                for (Task<T> f : futures) {
                    r = r & f.cancel(mayInterruptIfRunning);
                }
                return r;
            }

            @Override
            public boolean isCancelled() {
                boolean r = true;
                for (Task<T> f : futures) {
                    r = r & f.isCancelled();
                }
                return r;
            }

            @Override
            public boolean isDone() {
                boolean r = true;
                for (Task<T> f : futures) {
                    r = r & f.isDone();
                }
                return r;
            }

            @Override
            public List<T> get() throws InterruptedException, ExecutionException {
                waitAllLock.await();
                List<T> r = new ArrayList<T>();
                for (Task<T> f : futures) {
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
        final F.Action<T> action = new F.Action<T>() {

            public void invoke(T value) {
                waitAllLock.countDown();
                if(waitAllLock.getCount() == 0) {
                    try {
                        result.invoke(result.get());
                    } catch(Exception e) {
                        throw new UnexpectedException(e);
                    }
                }
            }

            public String toString() {
                return "waitAll.callback(countdown: " + waitAllLock.getCount() + ")";
            }
        };
        for (Task<T> f : futures) {
            f.onCompletion(action);
        }
        return result;
    }

    public static <T> Task<T> waitAny(final Task<T>... futures) {
        final Task<T> result = new Task<T>();

        final F.Action<T> action = new F.Action<T>() {
            boolean invoked = false;

            public void invoke(T value) {
                synchronized(this) {
                    if(invoked) {
                        return;
                    }
                    invoked = true;
                }
                result.invoke(value);
            }
            
        };
        for (Task<T> f : futures) {
            f.onCompletion(action);
        }
        return result;
    }

}
