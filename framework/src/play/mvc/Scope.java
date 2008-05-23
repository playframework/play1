package play.mvc;

import java.util.HashMap;

public class Scope {
    
    public static class Flash extends HashMap<String,String> {

        // ThreadLocal access
        private static ThreadLocal<Flash> current = new ThreadLocal<Flash>();    
        public static Flash get() {
            return current.get();
        }

    }
    
    public static class Session extends HashMap<String,String> {

        // ThreadLocal access
        public static ThreadLocal<Session> current = new ThreadLocal<Session>();    
        public static Session get() {
            return current.get();
        }

    }
    
    public static class Params extends HashMap<String, String[]> {
        
        // ThreadLocal access
        public static ThreadLocal<Params> current = new ThreadLocal<Params>();    
        public static Params get() {
            return current.get();
        }
        
    }
    
    public static class RenderArgs extends HashMap<String, String[]> {
        
        // ThreadLocal access
        public static ThreadLocal<RenderArgs> current = new ThreadLocal<RenderArgs>();    
        public static RenderArgs get() {
            return current.get();
        }
        
    }


}
