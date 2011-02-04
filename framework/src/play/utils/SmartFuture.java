package play.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SmartFuture<V> implements Future<V>, Action<V> {

    Future<V> innerFuture;

    public void wrap(Future<V> innerFuture) {
        this.innerFuture = innerFuture;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return innerFuture.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
        return innerFuture.isCancelled();
    }

    public boolean isDone() {
        return innerFuture.isDone();
    }

    public V get() throws InterruptedException, ExecutionException {
        return innerFuture.get();
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return innerFuture.get(timeout, unit);
    }

    // Callbacks
    
    private List<Action<V>> callbacks = new ArrayList<Action<V>>();
    private boolean invoked = false;
    private V result = null;

    public void invoke(V result) {
        synchronized(this) {
            if (!invoked) {
                invoked = true;
                this.result = result;
            }
        }
        for (Action<V> callback : callbacks) {
            callback.invoke(result);
        }
    }
    
    public void onCompletion(Action<V> callback) {
        synchronized(this) {
            if(!invoked) {
                callbacks.add(callback);
            }
        }
        if (invoked) {
            callback.invoke(result);
        }
    }
}
