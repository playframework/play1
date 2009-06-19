package play;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import java.util.concurrent.TimeUnit;
import play.Play.Mode;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;

/**
 * Run some code in a Play! context
 */
public class Invoker {

    public static ScheduledThreadPoolExecutor executor = null;

    /**
     * Run the code in a new thread took from a thread pool.
     * @param invocation The code to run
     * @return The future object, to know when the task is completed
     */
    public static Future invoke(final Invocation invocation) {
        return executor.submit(invocation);
    }

    /**
     * Run the code in a new thread after a delay
     * @param invocation The code to run
     * @param seconds The time to wait before
     * @return The future object, to know when the task is completed
     */
    public static Future invoke(final Invocation invocation, int seconds) {
        return executor.schedule(invocation, seconds, TimeUnit.SECONDS);
    }

    /**
     * Run the code in the same thread than caller.
     * @param invocation The code to run
     */
    public static void invokeInThread(Invocation invocation) {
        DirectInvocation directInvocation = new DirectInvocation(invocation);
        boolean retry = true;
        while(retry) {
            directInvocation.run();
            if(directInvocation.retry == -1) {
                retry = false;
            } else {
                try {
                    Thread.sleep(directInvocation.retry * 1000);
                } catch(InterruptedException e) {
                    throw new UnexpectedException(e);
                }
                retry = true;
            }
        }
    }

    /**
     * An Invocation in something to run in a Play! context
     */
    public static abstract class Invocation implements Runnable {

        /**
         * Override this method
         * @throws java.lang.Exception
         */
        public abstract void execute() throws Exception;

        /**
         * Things to do before an Invocation
         */
        public void before() {
            Thread.currentThread().setContextClassLoader(Play.classloader);
            Play.detectChanges();
            if (!Play.started) {
                if (Play.mode == Mode.PROD) {
                    throw new UnexpectedException("Application is not started");
                }
                Play.start();
            }
            Thread.currentThread().setContextClassLoader(Play.classloader);
            for (PlayPlugin plugin : Play.plugins) {
                plugin.beforeInvocation();
            }
        }

        /**
         * Things to do after an Invocation.
         * (if the Invocation code has not thrown any exception)
         */
        public void after() {
            for (PlayPlugin plugin : Play.plugins) {
                plugin.afterInvocation();
            }
            LocalVariablesNamesTracer.checkEmpty(); // detect bugs ....
        }

        /**
         * Things to do if the Invocation code thrown an exception
         */
        public void onException(Throwable e) {
            for (PlayPlugin plugin : Play.plugins) {
                plugin.onInvocationException(e);
            }
            if (e instanceof PlayException) {
                throw (PlayException) e;
            }
            throw new UnexpectedException(e);
        }

        /**
         * The request is suspended
         * @param timeout
         */
        public void suspend(int timeout) {
            Invoker.invoke(this, timeout);
        }

        /**
         * Things to do in all cases after the invocation.
         */
        public void _finally() {
            for (PlayPlugin plugin : Play.plugins) {
                plugin.invocationFinally();
            }
        }

        public void run() {
            try {
                before();
                execute();
                after();
            } catch (SuspendRequest e) {
                suspend(((SuspendRequest) e).timeout);
            } catch (Throwable e) {
                onException(e);
            } finally {
                _finally();
            }
        }
    }

    static class DirectInvocation extends Invocation {

        int retry = -1;
        Invocation originalInvocation;

        public DirectInvocation(Invocation originalInvocation) {
            this.originalInvocation = originalInvocation;
        }

        @Override
        public void execute() throws Exception {
            originalInvocation.execute();
        }

        @Override
        public void before() {
            retry = -1;
            originalInvocation.before();
        }

        @Override
        public void after() {
            originalInvocation.after();
        }

        @Override
        public void onException(Throwable e) {
            originalInvocation.onException(e);
        }

        @Override
        public void suspend(int timeout) {
            retry = timeout;
        }

        @Override
        public void _finally() {
            originalInvocation._finally();
        }

        @Override
        public void run() {
            originalInvocation.run();
        }

    }

    static {
        int core = Integer.parseInt(Play.configuration.getProperty("play.pool", Play.mode == Mode.DEV ? "1" : "3"));
        executor = new ScheduledThreadPoolExecutor(core, new ThreadPoolExecutor.AbortPolicy());
    }

    public static class SuspendRequest extends PlayException {

        int timeout;

        public SuspendRequest(int timeout) {
            this.timeout = timeout;
        }

        @Override
        public String getErrorTitle() {
            return "Request is suspended";
        }

        @Override
        public String getErrorDescription() {
            return "Retry in " + timeout + " s.";
        }
    }
}
