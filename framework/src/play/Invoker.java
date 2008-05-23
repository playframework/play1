package play;

public class Invoker {
    
    public static void invoke(Thread thread) {
        
        // 1. Reload
        Play.detectChanges();
        
        // 2. Prepare this thread
        thread.setContextClassLoader(Play.classloader);
        
        // 3. Run it
        thread.run();
        
    }

}
