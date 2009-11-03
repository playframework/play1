package play;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
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

    /**
     * Main executor for requests invocations.
     */
    public static ScheduledThreadPoolExecutor executor = null;

    /**
     * Run the code in a new thread took from a thread pool.
     * @param invocation The code to run
     * @return The future object, to know when the task is completed
     */
    public static Future invoke(final Invocation invocation) {
        Monitor monitor = MonitorFactory.getMonitor("Invoker queue size", "elmts.");
        monitor.add(executor.getQueue().size());
        invocation.waitInQueue = MonitorFactory.start("Waiting for execution");
        return executor.submit(invocation);
    }

    /**
     * Run the code in a new thread after a delay
     * @param invocation The code to run
     * @param seconds The time to wait before
     * @return The future object, to know when the task is completed
     */
    public static Future invoke(final Invocation invocation, int seconds) {
        Monitor monitor = MonitorFactory.getMonitor("Invocation queue", "elmts.");
        monitor.add(executor.getQueue().size());
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
         * If set, monitor the time the invocation waited in the queue
         */
        Monitor waitInQueue;

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
                } catch (Throwable ex) {
                }
            }
            if (e instanceof PlayException) {
                throw (PlayException) e;
            }
            throw new UnexpectedException(e);
        }

        /**
         * The request is suspended
         * @param suspendRequest
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

        /**
         * It's time to execute.
         */
        public void run() {
            if(waitInQueue != null) {
                waitInQueue.stop();
            }
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

    /**
     * A direct invocation (in the same thread than caller)
     */
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
    
    /**
     * Init executor at load time.
     */
    static {
        int core = Integer.parseInt(Play.configuration.getProperty("play.pool", Play.mode == Mode.DEV ? "1" : ((Runtime.getRuntime().availableProcessors()+1) + "")));
        executor = new ScheduledThreadPoolExecutor(core, new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * Throwable to indicate that the request must be suspended
     */
    public static class Suspend extends PlayException {

        /**
         * Suspend for a timeout (in seconds).
         */
        int timeout;
        
        /**
         * Wait for task execution.
         */
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

    /**
     * Utility that track tasks completion in order to resume suspended requests.
     */
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
