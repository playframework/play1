package play;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import play.Play.Mode;
import play.classloading.ApplicationClassloader;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.i18n.Lang;
import play.libs.F;
import play.libs.F.Promise;
import play.utils.PThreadFactory;

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
     * 
     * @param invocation
     *            The code to run
     * @return The future object, to know when the task is completed
     */
    public static Future<?> invoke(Invocation invocation) {
        Monitor monitor = MonitorFactory.getMonitor("Invoker queue size", "elmts.");
        monitor.add(executor.getQueue().size());
        invocation.waitInQueue = MonitorFactory.start("Waiting for execution");
        return executor.submit(invocation);
    }

    /**
     * Run the code in a new thread after a delay
     * 
     * @param invocation
     *            The code to run
     * @param millis
     *            The time to wait before, in milliseconds
     * @return The future object, to know when the task is completed
     */
    public static Future<?> invoke(Invocation invocation, long millis) {
        Monitor monitor = MonitorFactory.getMonitor("Invocation queue", "elmts.");
        monitor.add(executor.getQueue().size());
        return executor.schedule(invocation, millis, TimeUnit.MILLISECONDS);
    }

    /**
     * Run the code in the same thread than caller.
     * 
     * @param invocation
     *            The code to run
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
                        Thread.sleep(invocation.retry.timeout);
                    }
                } catch (Exception e) {
                    throw new UnexpectedException(e);
                }
                retry = true;
            }
        }
    }

    static void resetClassloaders() {
        Thread[] executorThreads = new Thread[executor.getPoolSize()];
        Thread.enumerate(executorThreads);
        for (Thread thread : executorThreads) {
            if (thread != null && thread.getContextClassLoader() instanceof ApplicationClassloader)
                thread.setContextClassLoader(ClassLoader.getSystemClassLoader());
        }
    }

    /**
     * The class/method that will be invoked by the current operation
     */
    public static class InvocationContext {

        public static final ThreadLocal<InvocationContext> current = new ThreadLocal<>();
        private final List<Annotation> annotations;
        private final String invocationType;

        public static InvocationContext current() {
            return current.get();
        }

        public InvocationContext(String invocationType) {
            this.invocationType = invocationType;
            this.annotations = new ArrayList<>();
        }

        public InvocationContext(String invocationType, List<Annotation> annotations) {
            this.invocationType = invocationType;
            this.annotations = annotations;
        }

        public InvocationContext(String invocationType, Annotation[] annotations) {
            this.invocationType = invocationType;
            this.annotations = Arrays.asList(annotations);
        }

        public InvocationContext(String invocationType, Annotation[]... annotations) {
            this.invocationType = invocationType;
            this.annotations = new ArrayList<>();
            for (Annotation[] some : annotations) {
                this.annotations.addAll(Arrays.asList(some));
            }
        }

        public List<Annotation> getAnnotations() {
            return annotations;
        }

        @SuppressWarnings("unchecked")
        public <T extends Annotation> T getAnnotation(Class<T> clazz) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().isAssignableFrom(clazz)) {
                    return (T) annotation;
                }
            }
            return null;
        }

        public <T extends Annotation> boolean isAnnotationPresent(Class<T> clazz) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().isAssignableFrom(clazz)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns the InvocationType for this invocation - Ie: A plugin can use this to find out if it runs in the
         * context of a background Job
         * 
         * @return the InvocationType for this invocation
         */
        public String getInvocationType() {
            return invocationType;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("InvocationType: ");
            builder.append(invocationType);
            builder.append(". annotations: ");
            for (Annotation annotation : annotations) {
                builder.append(annotation.toString()).append(",");
            }
            return builder.toString();
        }
    }

    /**
     * An Invocation in something to run in a Play! context
     */
    public abstract static class Invocation implements Runnable {

        /**
         * If set, monitor the time the invocation waited in the queue
         */
        Monitor waitInQueue;

        /**
         * Override this method
         * 
         * @throws java.lang.Exception
         *             Thrown if Invocation encounters any problems
         */
        public abstract void execute() throws Exception;

        /**
         * Needs this method to do stuff *before* init() is executed. The different Invocation-implementations does a
         * lot of stuff in init() and they might do it before calling super.init()
         */
        protected void preInit() {
            // clear language for this request - we're resolving it later when it is needed
            Lang.clear();
        }

        /**
         * Init the call (especially useful in DEV mode to detect changes)
         * 
         * @return true if successful
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

        public abstract InvocationContext getInvocationContext();

        /**
         * Things to do before an Invocation
         */
        public void before() {
            Thread.currentThread().setContextClassLoader(Play.classloader);
            Play.pluginCollection.beforeInvocation();
        }

        /**
         * Things to do after an Invocation. (if the Invocation code has not thrown any exception)
         */
        public void after() {
            Play.pluginCollection.afterInvocation();
            LocalVariablesNamesTracer.checkEmpty(); // detect bugs ....
        }

        /**
         * Things to do when the whole invocation has succeeded (before + execute + after)
         * 
         * @throws java.lang.Exception
         *             Thrown if Invoker encounters any problems
         */
        public void onSuccess() throws Exception {
            Play.pluginCollection.onInvocationSuccess();
        }

        /**
         * Things to do if the Invocation code thrown an exception
         * 
         * @param e
         *            The exception
         */
        public void onException(Throwable e) {
            Play.pluginCollection.onInvocationException(e);
            if (e instanceof PlayException) {
                throw (PlayException) e;
            }
            throw new UnexpectedException(e);
        }

        /**
         * The request is suspended
         * 
         * @param suspendRequest
         *            the suspended request
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
            Play.pluginCollection.invocationFinally();
            InvocationContext.current.remove();
        }

        private void withinFilter(play.libs.F.Function0<Void> fct) throws Throwable {
            F.Option<PlayPlugin.Filter<Void>> filters = Play.pluginCollection.composeFilters();
            if (filters.isDefined()) {
                filters.get().withinFilter(fct);
            }
        }

        /**
         * It's time to execute.
         */
        @Override
        public void run() {
            if (waitInQueue != null) {
                waitInQueue.stop();
            }
            try {
                preInit();
                if (init()) {
                    before();
                    final AtomicBoolean executed = new AtomicBoolean(false);
                    this.withinFilter(new play.libs.F.Function0<Void>() {
                        @Override
                        public Void apply() throws Throwable {
                            executed.set(true);
                            execute();
                            return null;
                        }
                    });
                    // No filter function found => we need to execute anyway( as before the use of withinFilter )
                    if (!executed.get()) {
                        execute();
                    }
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
    public abstract static class DirectInvocation extends Invocation {

        public static final String invocationType = "DirectInvocation";

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

        @Override
        public InvocationContext getInvocationContext() {
            return new InvocationContext(invocationType);
        }
    }

    /**
     * Init executor at load time.
     */
    static {
        int core = Integer.parseInt(Play.configuration.getProperty("play.pool",
                Play.mode == Mode.DEV ? "1" : ((Runtime.getRuntime().availableProcessors() + 1) + "")));
        executor = new ScheduledThreadPoolExecutor(core, new PThreadFactory("play"), new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * Throwable to indicate that the request must be suspended
     */
    public static class Suspend extends PlayException {

        /**
         * Suspend for a timeout (in milliseconds).
         */
        long timeout;

        /**
         * Wait for task execution.
         */
        Future<?> task;

        public Suspend(long timeout) {
            this.timeout = timeout;
        }

        public Suspend(Future<?> task) {
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
            return "Retry in " + timeout + " ms.";
        }
    }

    /**
     * Utility that track tasks completion in order to resume suspended requests.
     */
    static class WaitForTasksCompletion extends Thread {

        static WaitForTasksCompletion instance;
        Map<Future<?>, Invocation> queue;

        public WaitForTasksCompletion() {
            queue = new ConcurrentHashMap<>();
            setName("WaitForTasksCompletion");
            setDaemon(true);
        }

        public static <V> void waitFor(Future<V> task, final Invocation invocation) {
            if (task instanceof Promise) {
                Promise<V> smartFuture = (Promise<V>) task;
                smartFuture.onRedeem(new F.Action<F.Promise<V>>() {
                    @Override
                    public void invoke(Promise<V> result) {
                        executor.submit(invocation);
                    }
                });
            } else {
                synchronized (WaitForTasksCompletion.class) {
                    if (instance == null) {
                        instance = new WaitForTasksCompletion();
                        Logger.warn("Start WaitForTasksCompletion");
                        instance.start();
                    }
                    instance.queue.put(task, invocation);
                }
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    if (!queue.isEmpty()) {
                        for (Future<?> task : new HashSet<>(queue.keySet())) {
                            if (task.isDone()) {
                                executor.submit(queue.get(task));
                                queue.remove(task);
                            }
                        }
                    }
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    Logger.warn(ex, "While waiting for task completions");
                }
            }
        }
    }
}
