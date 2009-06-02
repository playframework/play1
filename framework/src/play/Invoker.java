package play;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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

    public static ThreadPoolExecutor executor = null;
    public static BlockingQueue<Runnable> queue = null;
    /**
     * Run the code in a new thread took from a thread pool.
     * @param invocation The code to run
     */
    public static void invoke(final Invocation invocation) {
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
        public static void before() {
            Play.detectChanges();
            if (!Play.started) {
                if(Play.mode == Mode.PROD) {
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
        public static void after() {
            for (PlayPlugin plugin : Play.plugins) {
                plugin.afterInvocation();
            }          
            LocalVariablesNamesTracer.checkEmpty(); // detect bugs ....
        }

        /**
         * Things to do if the Invocation code thrown an exception
         */
        public static void onException(Throwable e) {
            for (PlayPlugin plugin : Play.plugins) {
                plugin.onInvocationException(e);
            }            
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

        public void doIt() {
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

    static {
        Properties p = Play.configuration;
        int queueSize = Integer.parseInt(p.getProperty("play.pool.queue", "200"));
        int core = Integer.parseInt(p.getProperty("play.pool.core", "2"));
        int max = Integer.parseInt(p.getProperty("play.pool.max", "10"));
        int keepalive = Integer.parseInt(p.getProperty("play.pool.keepalive", "5"));
        queue = new LinkedBlockingQueue<Runnable>(queueSize);
        executor = new ThreadPoolExecutor(core, max, keepalive * 60, TimeUnit.SECONDS, queue, new ThreadPoolExecutor.AbortPolicy());
    }
}
