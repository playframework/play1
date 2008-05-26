package play;

import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.db.jpa.Jpa;

public class Invoker {
    
    public static void invoke(Invocation invocation) {
        Play.detectChanges();
        invocation.start();        
    }
    
    public static abstract class Invocation extends Thread {
    
        public abstract void execute();

        @Override
        public void run() {
            setContextClassLoader(Play.classloader);
            LocalVariablesNamesTracer.enterMethod();
            if (Jpa.isEnabled()) Jpa.startTx(false);
            execute();
            if (Jpa.isEnabled()) Jpa.closeTx();
        }

    }

}
