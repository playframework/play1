package play;

public class Logger {
    
    public static org.apache.log4j.Logger log4j = org.apache.log4j.Logger.getLogger("play");
 
    public static void trace(String message, Object... args) { 
        log4j.trace(String.format(message, args));
    }
    
    public static void trace(Throwable e, String message, Object... args) { 
        log4j.trace(String.format(message, args), e);
    }
    
    public static void debug(String message, Object... args) { 
        log4j.debug(String.format(message, args));
    }
    
    public static void debug(Throwable e, String message, Object... args) { 
        log4j.debug(String.format(message, args), e);
    }
    
    public static void info(String message, Object... args) { 
        log4j.info(String.format(message, args));
    }
    
    public static void info(Throwable e, String message, Object... args) { 
        log4j.info(String.format(message, args), e);
    }
    
    public static void warn(String message, Object... args) { 
        log4j.warn(String.format(message, args));
    }
    
    public static void warn(Throwable e, String message, Object... args) { 
        log4j.warn(String.format(message, args), e);
    }

    public static void error(String message, Object... args) { 
        log4j.error(String.format(message, args));
    }
    
    public static void error(Throwable e, String message, Object... args) { 
        log4j.error(String.format(message, args), e);
    }
    
    public static void fatal(String message, Object... args) { 
        log4j.fatal(String.format(message, args));
    }
    
    public static void fatal(Throwable e, String message, Object... args) { 
        log4j.fatal(String.format(message, args), e);
    }

}
