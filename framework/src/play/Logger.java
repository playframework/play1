package play;

public class Logger {
    
    static Long start = System.currentTimeMillis();
    
    public static void debug(String message, Object... args) { 
        print("DEBUG", message, args);
    }

    public static void debug(Throwable e) { 
        e.printStackTrace();
    }
    
    public static void info(String message, Object... args) {    
        print("INFO", message, args);
    }
    
    public static void warn(String message, Object... args) {
        print("WARN", message, args);
    }
    
    public static void error(String message, Object... args) {
        print("ERROR", message, args);
    }
    
    public static void fatal(String message, Object... args) {
        print("FATAL", message, args);
    }
    
    private static void print(String level, String message, Object... args) {
        //if(!level.equals("DEBUG")) {
            System.out.println(String.format("%-6s %-6s %s", time(), level, String.format(message, args)));
        //}
    }
   
    private static Long time() {
        return System.currentTimeMillis() - start;
    }

}
