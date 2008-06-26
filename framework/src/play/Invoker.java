package play;

import java.util.Properties;
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
import play.i18n.Locale;

public class Invoker {

    public static Executor executor = null;

    public static void invoke(Invocation invocation) {
        if (executor == null) {
            executor = Invoker.startExecutor();
        }
        executor.execute(invocation);
    }

    public static void invokeInThread(Invocation invocation) {
        invocation.run();
    }

    public static abstract class Invocation extends Thread {

        public abstract void execute() throws Exception;

        public static void before() {
            Thread.currentThread().setContextClassLoader(Play.classloader);
            LocalVariablesNamesTracer.enterMethod();
            JPA.startTx(false);
            if (Play.locales.isEmpty()) {
                Locale.set("");
            } else {
                Locale.set(Play.locales.get(0));
            }
        }
        
        public static void after() {
            JPA.closeTx(false);
        }
        
        public static void onException(Throwable e) {
            JPA.closeTx(true);
            if (e instanceof PlayException) {
                throw (PlayException) e;
            }
            throw new UnexpectedException(e);
        }
        
        public static void _finally() {
            DB.close();
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
