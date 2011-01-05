package play;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import java.util.ArrayList;

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
    public static Future<?> invoke(final Invocation invocation) {
        Monitor monitor = MonitorFactory.getMonitor("Invoker queue size", "elmts.");
        monitor.add(executor.getQueue().size());
        invocation.waitInQueue = MonitorFactory.start("Waiting for execution");
        return executor.submit(invocation);
    }

    /**
     * Run the code in a new thread after a delay
     * @param invocation The code to run
     * @param millis The time to wait before, in milliseconds
     * @return The future object, to know when the task is completed
     */
    public static Future<?> invoke(final Invocation invocation, long millis) {
        Monitor monitor = MonitorFactory.getMonitor("Invocation queue", "elmts.");
        monitor.add(executor.getQueue().size());
        return executor.schedule(invocation, millis, TimeUnit.MILLISECONDS);
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
                    if (invocation.retry.tasks != null) {
                        for (Future<?> f : invocation.retry.tasks) {
                            f.get();
                        }
                    } else {
                        Thread.sleep(invocation.retry.timeout);
                    }
                } catch (Exception e) {
                    throw new UnexpectedException(e);
                }
                retry = true;
            }
        }
    }

    /**
     * The class/method that will be invoked by the current operation
     */
    public static class InvocationContext {

        public static ThreadLocal<InvocationContext> current = new ThreadLocal<InvocationContext>();
        private List<Annotation> annotations = new ArrayList<Annotation>();

        public static InvocationContext current() {
            return current.get();
        }        
        
        public InvocationContext(List<Annotation> annotations) {
            this.annotations = annotations;
        }

        public InvocationContext(Annotation[] annotations) {
            this.annotations = Arrays.asList(annotations);
        }

        public InvocationContext(Annotation[]... annotations) {
            for(Annotation[] some : annotations) {
                this.annotations.addAll(Arrays.asList(some));
            }
        }

        public List<Annotation> getAnnotations() {
            return annotations;
        }

        @SuppressWarnings("unchecked")
        public <T extends Annotation> T getAnnotation(Class<T> clazz) {
            for(Annotation annotation : annotations) {
                if(annotation.annotationType().isAssignableFrom(clazz)) {
                    return (T)annotation;
                }
            }
            return null;
        }

        public <T extends Annotation> boolean isAnnotationPresent(Class<T> clazz) {
            for(Annotation annotation : annotations) {
                if(annotation.annotationType().isAssignableFrom(clazz)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for(Annotation annotation : annotations) {
                builder.append(annotation.toString()).append(",");
            }
            return builder.toString();
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
            InvocationContext.current.set(getInvocationContext());
            return true;
        }

        public InvocationContext getInvocationContext() {
            return new InvocationContext(new ArrayList<Annotation>());
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
         * Things to do when the whole invocation has succeeded (before + execute + after)
         */
        public void onSuccess() throws Exception {
            for (PlayPlugin plugin : Play.plugins) {
                plugin.onInvocationSuccess();
            }
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
            if (suspendRequest.tasks != null) {
                WaitForTasksCompletion.waitFor(suspendRequest.tasks, this);
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
            if (waitInQueue != null) {
                waitInQueue.stop();
            }
            try {
                if (init()) {
                    before();
                    execute();
                    after();
                    onSuccess();
                }
            } catch (Suspend e) {
                suspend(e);
                after();
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
        int core = Integer.parseInt(Play.configuration.getProperty("play.pool", Play.mode == Mode.DEV ? "1" : ((Runtime.getRuntime().availableProcessors() + 1) + "")));
        executor = new ScheduledThreadPoolExecutor(core, new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * Throwable to indicate that the request must be suspended
     */
    public static class Suspend extends PlayException {
        private static final long serialVersionUID = 1L;
				/**
         * Suspend for a timeout (in milliseconds).
         */
        long timeout;
        /**
         * Wait for task execution.
         */
        List<Future<?>> tasks;

        public Suspend(long timeout) {
            this.timeout = timeout;
        }

        public Suspend(Future<?>... tasks) {
            this.tasks = Arrays.asList(tasks);
        }

        @Override
        public String getErrorTitle() {
            return "Request is suspended";
        }

        @Override
        public String getErrorDescription() {
            if (tasks != null) {
                return "Wait for " + tasks;
            }
            return "Retry in " + timeout + " ms.";
        }
    }

    /**
     * Utility that track tasks completion in order to resume suspended requests.
     */
    static class WaitForTasksCompletion extends Thread {

        Map<List<Future<?>>, Invocation> queue;
        static WaitForTasksCompletion instance;

        public WaitForTasksCompletion() {
            queue = new ConcurrentHashMap<List<Future<?>>, Invocation>();
            setName("WaitForTasksCompletion");
            setDaemon(true);
            start();
        }

        public static void waitFor(List<Future<?>> tasks, Invocation invocation) {
            if (instance == null) {
                instance = new WaitForTasksCompletion();
            }
            instance.queue.put(tasks, invocation);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    if (!queue.isEmpty()) {
                        for (List<Future<?>> tasks : new HashSet<List<Future<?>>>(queue.keySet())) {
                            boolean allDone = true;
                            for (Future<?> f : tasks) {
                                if (!f.isDone()) {
                                    allDone = false;
                                }
                            }
                            if (allDone) {
                                executor.submit(queue.get(tasks));
                                queue.remove(tasks);
                            }
                        }
                    }
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    Logger.warn(ex, "");
                }
            }
        }
    }
}
