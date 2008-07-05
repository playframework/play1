package play;

import java.util.Properties;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.db.DB;
import play.db.jpa.JPA;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.i18n.Lang;

/**
 * Run some code in a Play! context
 */
public class Invoker {

    static Executor executor = null;

    /**
     * Run the code in a new thread took from a thread pool.
     * @param invocation The code to run
     */
    public static void invoke(Invocation invocation) {
        if (executor == null) {
            executor = Invoker.startExecutor();
        }
        executor.execute(invocation);
    }

    /**
     * Run the code in the same thread than caller.
     * @param invocation The code to run
     */
    public static void invokeInThread(Invocation invocation) {
        invocation.run();
    }

    /**
     * An Invocation in something to run in a Play! context
     */
    public static abstract class Invocation extends Thread {

        /**
         * Override this method
         * @throws java.lang.Exception
         */
        public abstract void execute() throws Exception;

        /**
         * Things to do before an Invocation
         */
        public static void before() {
            Thread.currentThread().setContextClassLoader(Play.classloader);
            LocalVariablesNamesTracer.clear();
            LocalVariablesNamesTracer.enterMethod();
            Play.detectChanges();
            if (!Play.started) {
                Play.start();
            }
            for (PlayPlugin plugin : Play.plugins) {
                plugin.beforeInvocation();                
            }
            JPA.startTx(false);
            if (Play.locales.isEmpty()) {
                Lang.set("");
            } else {
                Lang.set(Play.locales.get(0));
            }
        }

        /**
         * Things to do after an Invocation.
         * (if the Invocation code has not thrown any exception)
         */
        public static void after() {
            for (PlayPlugin plugin : Play.plugins) {
                plugin.afterInvocation();                
            }
            // TODO: move these as plugin -->
            JPA.closeTx(false);
            LocalVariablesNamesTracer.exitMethod();
        }

        /**
         * Things to do if the Invocation code thrown an exception
         */
        public static void onException(Throwable e) {
            for (PlayPlugin plugin : Play.plugins) {
                plugin.onInvocationException(e);                
            }
            // TODO: move these as plugin -->
            JPA.closeTx(true);
            LocalVariablesNamesTracer.exitMethod();
            if (e instanceof PlayException) {
                throw (PlayException) e;
            }
            throw new UnexpectedException(e);
        }

        /**
         * Things to do in all cases after the invocation.
         */
        public static void _finally() {
            for (PlayPlugin plugin : Play.plugins) {
                plugin.invocationFinally();                
            }
        }

        @Override
        public void run() {
            try {
                before();
                execute();
                after();
            } catch (Throwable e) {
                onException(e);
            } finally {
                _finally();

            }
        }
    }

    private static Executor startExecutor() {
        Properties p = Play.configuration;
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
        int core = Integer.parseInt(p.getProperty("play.pool.core", "2"));
        int max = Integer.parseInt(p.getProperty("play.pool.max", "50"));
        int keepalive = Integer.parseInt(p.getProperty("play.pool.keepalive", "5"));
        return new ThreadPoolExecutor(core, max, keepalive * 60, TimeUnit.SECONDS, queue, new ThreadPoolExecutor.AbortPolicy());
    }
}
