package play;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
    public static void invokeInThread(DirectInvocation invocation) {
        boolean retry = true;
        while (retry) {
            invocation.run();
            if (invocation.retry == null) {
                retry = false;
            } else {
                try {
                    if (invocation.retry.task != null) {
                        invocation.retry.task.get();
                    } else {
                        Thread.sleep(invocation.retry.timeout * 1000);
                    }
                } catch (Exception e) {
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
         * Init the call (especially usefull in DEV mode to detect changes)
         */
        public boolean init() {
            Thread.currentThread().setContextClassLoader(Play.classloader);
            Play.detectChanges();
            if (!Play.started) {
                if (Play.mode == Mode.PROD) {
                    throw new UnexpectedException("Application is not started");
                }
                Play.start();
            }
            return true;
        }

        /**
         * Things to do before an Invocation
         */
        public void before() {
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
                try {
                    plugin.onInvocationException(e);
                } catch(Throwable ex) {
                }
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
        public void suspend(Suspend suspendRequest) {
            if (suspendRequest.task != null) {
                WaitForTasksCompletion.waitFor(suspendRequest.task, this);
            } else {
                Invoker.invoke(this, suspendRequest.timeout);
            }
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
                if (init()) {
                    before();
                    execute();
                    after();
                }
            } catch (Suspend e) {
                suspend(((Suspend) e));
            } catch (Throwable e) {
                onException(e);
            } finally {
                _finally();
            }
        }
    }

    public static abstract class DirectInvocation extends Invocation {

        Suspend retry = null;

        @Override
        public boolean init() {
            retry = null;
            return super.init();
        }

        @Override
        public void suspend(Suspend suspendRequest) {
            retry = suspendRequest;
        }

    }
    

    static {
        int core = Integer.parseInt(Play.configuration.getProperty("play.pool", Play.mode == Mode.DEV ? "1" : "3"));
        executor = new ScheduledThreadPoolExecutor(core, new ThreadPoolExecutor.AbortPolicy());
    }

    public static class Suspend extends PlayException {

        int timeout;
        Future task;

        public Suspend(int timeout) {
            this.timeout = timeout;
        }

        public Suspend(Future task) {
            this.task = task;
        }

        @Override
        public String getErrorTitle() {
            return "Request is suspended";
        }

        @Override
        public String getErrorDescription() {
            if (task != null) {
                return "Wait for " + task;
            }
            return "Retry in " + timeout + " s.";
        }
    }

    static class WaitForTasksCompletion extends Thread {

        Map<Future, Invocation> queue;
        static WaitForTasksCompletion instance;

        public WaitForTasksCompletion() {
            queue = new HashMap();
            setName("WaitForTasksCompletion");
            setDaemon(true);
            start();
        }

        public static void waitFor(Future task, Invocation invocation) {
            if (instance == null) {
                instance = new WaitForTasksCompletion();
            }
            instance.queue.put(task, invocation);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    if (!queue.isEmpty()) {
                        for (Future task : new HashSet<Future>(queue.keySet())) {
                            if (task.isDone()) {
                                executor.submit(queue.get(task));
                                queue.remove(task);
                            }
                        }
                    }
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.warn(ex, "");
                }
            }
        }
    }
}
