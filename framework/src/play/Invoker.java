package play;

import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;

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
            execute();
        }

    }

}
