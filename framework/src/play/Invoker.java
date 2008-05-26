package play;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.db.jpa.Jpa;

public class Invoker {
    public static Executor executor =null;
       
    public static void invoke(Invocation invocation) {
        Play.detectChanges();
        if (executor==null)
            executor=Invoker.startExecutor();
        executor.execute(invocation);      
    }

    public static void invokeInThread(Invocation invocation) {
        Play.detectChanges();
        invocation.run();      
    }
    
    public static abstract class Invocation extends Thread {
    
        public abstract void execute();

        @Override
        public void run() {
            setContextClassLoader(Play.classloader);
            LocalVariablesNamesTracer.enterMethod();
            if (Jpa.isEnabled()) Jpa.startTx(false);
            try {
                execute();
            } catch (Throwable e) {
                if (Jpa.isEnabled()) Jpa.closeTx(true);
                throw new RuntimeException(e);
            }
            if (Jpa.isEnabled()) Jpa.closeTx(false);
        }
    }

    private static Executor startExecutor () {
        Properties p = Play.configuration;
        BlockingQueue queue = new LinkedBlockingQueue ();
        int core = Integer.parseInt(p.getProperty("play.pool.core", "2"));
        int max = Integer.parseInt(p.getProperty("play.pool.max", "20"));
        int keepalive = Integer.parseInt(p.getProperty("play.pool.keepalive", "5"));
        return new ThreadPoolExecutor (core,max,keepalive*60,TimeUnit.SECONDS,queue,new ThreadPoolExecutor.AbortPolicy());
    }
}
