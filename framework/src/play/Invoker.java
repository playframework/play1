package play;

import java.util.concurrent.RejectedExecutionException;
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
     */
    public static void invoke(final Invocation invocation) {
        // TODO: check the queue size ?
        executor.execute(new Thread() {

            @Override
            public void run() {
                invocation.doIt();
            }
        });
    }

    /**
     * Run the code in the same thread than caller.
     * @param invocation The code to run
     */
    public static void invokeInThread(Invocation invocation) {
        invocation.doIt();
    }

    /**
     * An Invocation in something to run in a Play! context
     */
    public static abstract class Invocation {

        /**
         * Override this method
         * @throws java.lang.Exception
         */
        public abstract void execute() throws Exception;

        /**
         * Things to do before an Invocation
         */
        public void before() {
            Play.detectChanges();
            if (!Play.started) {
                if (Play.mode == Mode.PROD) {
                    throw new UnexpectedException("Application is not started");
                }
                Play.start();
            }
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
            final Invocation invocation = this;
            executor.schedule(new Thread() {

                @Override
                public void run() {
                    invocation.doIt();
                }
            }, timeout, TimeUnit.SECONDS);
        }

        /**
         * Things to do in all cases after the invocation.
         */
        public void _finally() {
            for (PlayPlugin plugin : Play.plugins) {
                plugin.invocationFinally();
            }
        }

        public void doIt() {
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
    

    static {
        int core = Integer.parseInt(Play.configuration.getProperty("play.pool", "1"));
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
