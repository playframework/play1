package play;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Priority;
import play.exceptions.PlayException;

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
        if(!niceThrowable(Priority.DEBUG, e, message, args)) log4j.debug(String.format(message, args), e);
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
        if(!niceThrowable(Priority.INFO, e, message, args)) log4j.info(String.format(message, args), e);
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
        if(!niceThrowable(Priority.WARN, e, message, args)) log4j.warn(String.format(message, args), e);
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
        if(!niceThrowable(Priority.ERROR, e, message, args)) log4j.error(String.format(message, args), e);
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
        if(!niceThrowable(Priority.FATAL, e, message, args)) log4j.fatal(String.format(message, args), e);
    }
    
    
    // If e is a PlayException -> a very clean report
    static boolean niceThrowable(Priority priority, Throwable e, String message, Object... args) {
        if (e instanceof PlayException) {
            
            Throwable toClean = e;
            for(int i=0; i<5; i++) {
                // Clean stack trace
                List<StackTraceElement> cleanTrace = new ArrayList<StackTraceElement>();
                for(StackTraceElement se : toClean.getStackTrace()) {
                    if(se.getClassName().startsWith("org.apache.mina.")) {
                        cleanTrace.add(new StackTraceElement("Play!", "HTTP Server", "Mina", -1));
                        break;
                    }
                    cleanTrace.add(se);
                }
                toClean.setStackTrace(cleanTrace.toArray(new StackTraceElement[cleanTrace.size()]));
                toClean = toClean.getCause();
                if(toClean == null) break;
            }
            
            PlayException playException = (PlayException) e;
            StringWriter sw = new StringWriter();
            PrintWriter errorOut = new PrintWriter(sw);
            
            
            errorOut.println("");
            errorOut.println("");
            errorOut.println("@" + playException.getId());
            errorOut.println(String.format(message, args));
            errorOut.println("");
            if(playException.isSourceAvailable()) {
                errorOut.println(playException.getErrorTitle() + " (In " + playException.getSourceFile() + " around line " + playException.getLineNumber() +")");
            
            } else {
                errorOut.println(playException.getErrorTitle());            
            }
            errorOut.println(playException.getErrorDescription().replace("<strong>", "").replace("</strong>", "").replace("\n", " "));
            log4j.log(priority, sw.toString(), e);
            return true;
        }
        return false;
    }
}
