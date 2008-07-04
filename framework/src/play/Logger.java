package play;

/**
 * Main logger of the application.
 * Free to use from the aplication code.
 */
public class Logger {
    
    /**
     * The application logger (play).
     */
    public static org.apache.log4j.Logger log4j = org.apache.log4j.Logger.getLogger("play");
 
    /**
     * Log with TRACE level
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void trace(String message, Object... args) { 
        log4j.trace(String.format(message, args));
    }

    /**
     * Log with TRACE level
     * @param e the exception to log
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void trace(Throwable e, String message, Object... args) { 
        log4j.trace(String.format(message, args), e);
    }
    
    /**
     * Log with DEBUG level
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void debug(String message, Object... args) { 
        log4j.debug(String.format(message, args));
    }
    
    /**
     * Log with DEBUG level
     * @param e the exception to log
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void debug(Throwable e, String message, Object... args) { 
        log4j.debug(String.format(message, args), e);
    }    
    
    /**
     * Log with INFO level
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void info(String message, Object... args) { 
        log4j.info(String.format(message, args));
    }

    /**
     * Log with INFO level
     * @param e the exception to log
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void info(Throwable e, String message, Object... args) { 
        log4j.info(String.format(message, args), e);
    }
    
    
    /**
     * Log with WARN level
     * @param message The message pattern
     * @param args Pattern arguments
     */        
    public static void warn(String message, Object... args) { 
        log4j.warn(String.format(message, args));
    }

    /**
     * Log with WARN level
     * @param e the exception to log
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void warn(Throwable e, String message, Object... args) { 
        log4j.warn(String.format(message, args), e);
    }

    /**
     * Log with ERROR level
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void error(String message, Object... args) { 
        log4j.error(String.format(message, args));
    }
    
    /**
     * Log with ERROR level
     * @param e the exception to log
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void error(Throwable e, String message, Object... args) { 
        log4j.error(String.format(message, args), e);
    }
    
    /**
     * Log with FATAL level
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void fatal(String message, Object... args) { 
        log4j.fatal(String.format(message, args));
    }
    
    /**
     * Log with FATAL level
     * @param e the exception to log
     * @param message The message pattern
     * @param args Pattern arguments
     */    
    public static void fatal(Throwable e, String message, Object... args) { 
        log4j.fatal(String.format(message, args), e);
    }

}
