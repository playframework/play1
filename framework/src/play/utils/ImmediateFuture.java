package play.utils;

import java.util.concurrent.*;

public final class ImmediateFuture implements Future<Boolean> {
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return true;
    }

    public Boolean get() throws InterruptedException, ExecutionException {
        return true;
    }

    public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return true;
    }
}
