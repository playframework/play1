package play;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Priority;
import org.apache.log4j.PropertyConfigurator;
import play.exceptions.PlayException;
import play.mvc.Http.Request;

/**
 * Main logger of the application.
 * Free to use from the aplication code.
 */
public class Logger {

    public static boolean forceJuli = false;
    public static boolean redirectJuli = false;
    public static boolean recordCaller = false;
    /**
     * The application logger (play).
     */
    public static org.apache.log4j.Logger log4j;
    public static java.util.logging.Logger juli = java.util.logging.Logger.getLogger("play");
    

    static {
        URL log4jConf = Logger.class.getResource("/log4j.properties");
        if (log4jConf == null) {
            Properties shutUp = new Properties();
            shutUp.setProperty("log4j.rootLogger", "OFF");
            PropertyConfigurator.configure(shutUp);
        } else {
            PropertyConfigurator.configure(log4jConf);
            Logger.log4j = org.apache.log4j.Logger.getLogger("play");
            if (Play.id.equals("test")) {
                org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
                try {
                    if (!Play.getFile("test-result").exists()) {
                        Play.getFile("test-result").mkdir();
                    }
                    Appender testLog = new FileAppender(new PatternLayout("%d{DATE} %-5p ~ %m%n"), Play.getFile("test-result/application.log").getAbsolutePath(), false);
                    rootLogger.addAppender(testLog);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public static void setUp(String level) {
        if (forceJuli || log4j == null) {
            Logger.juli.setLevel(toJuliLevel(level));
        } else {
            Logger.log4j.setLevel(org.apache.log4j.Level.toLevel(level));
            if (redirectJuli) {
                java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
                for (Handler handler : rootLogger.getHandlers()) {
                    rootLogger.removeHandler(handler);
                }
                Handler activeHandler = new JuliToLog4jHandler();
                java.util.logging.Level juliLevel = toJuliLevel(level);
                activeHandler.setLevel(juliLevel);
                rootLogger.addHandler(activeHandler);
                rootLogger.setLevel(juliLevel);
            }
            if(Play.configuration != null) {
                recordCaller = Boolean.parseBoolean(Play.configuration.getProperty("application.log.recordCaller", "false"));
            }
        }
    }

    static java.util.logging.Level toJuliLevel(String level) {
        java.util.logging.Level juliLevel = java.util.logging.Level.INFO;
        if (level.equals("ERROR") || level.equals("FATAL")) {
            juliLevel = java.util.logging.Level.SEVERE;
        }
        if (level.equals("WARN")) {
            juliLevel = java.util.logging.Level.WARNING;
        }
        if (level.equals("DEBUG")) {
            juliLevel = java.util.logging.Level.FINE;
        }
        if (level.equals("TRACE")) {
            juliLevel = java.util.logging.Level.FINEST;
        }
        if (level.equals("ALL")) {
            juliLevel = java.util.logging.Level.ALL;
        }
        if (level.equals("OFF")) {
            juliLevel = java.util.logging.Level.OFF;
        }
        return juliLevel;
    }

    /**
     * Log with TRACE level
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void trace(String message, Object... args) {
        if (forceJuli || log4j == null) {
            try {
                juli.finest(format(message, args));
            } catch (Throwable ex) {
                juli.log(Level.SEVERE, "Oops. Error in Logger !", ex);
            }
        } else {
            try {
                if (recordCaller) {
                    CallInfo ci = getCallerInformations(3);
                    log4j.getLogger(ci.className).trace(format(message, args));
                } else {
                    log4j.trace(format(message, args));
                }
            } catch (Throwable ex) {
                log4j.error("Oops. Error in Logger !", ex);
            }
        }
    }

    /**
     * Log with DEBUG level
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void debug(String message, Object... args) {
        if (forceJuli || log4j == null) {
            try {
                juli.fine(format(message, args));
            } catch (Throwable ex) {
                juli.log(Level.SEVERE, "Oops. Error in Logger !", ex);
            }
        } else {
            try {
                if (recordCaller) {
                    CallInfo ci = getCallerInformations(3);
                    log4j.getLogger(ci.className).debug(format(message, args));
                } else {
                    log4j.debug(format(message, args));
                }
            } catch (Throwable ex) {
                log4j.error("Oops. Error in Logger !", ex);
            }
        }
    }

    /**
     * Log with DEBUG level
     * @param e the exception to log
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void debug(Throwable e, String message, Object... args) {
        if (forceJuli || log4j == null) {
            try {
                if (!niceThrowable(Priority.DEBUG, e, message, args)) {
                    juli.log(Level.CONFIG, format(message, args), e);
                }
            } catch (Throwable ex) {
                juli.log(Level.SEVERE, "Oops. Error in Logger !", ex);
            }
        } else {
            try {
                if (!niceThrowable(Priority.DEBUG, e, message, args)) {
                    if (recordCaller) {
                        CallInfo ci = getCallerInformations(3);
                        log4j.getLogger(ci.className).debug(format(message, args), e);
                    } else {
                        log4j.debug(format(message, args), e);
                    }
                }
            } catch (Throwable ex) {
                log4j.error("Oops. Error in Logger !", ex);
            }
        }
    }

    /**
     * Log with INFO level
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void info(String message, Object... args) {
        if (forceJuli || log4j == null) {
            try {
                juli.info(format(message, args));
            } catch (Throwable ex) {
                juli.log(Level.SEVERE, "Oops. Error in Logger !", ex);
            }
        } else {
            try {
                if (recordCaller) {
                    CallInfo ci = getCallerInformations(3);
                    log4j.getLogger(ci.className).info(format(message, args));
                } else {
                    log4j.info(format(message, args));
                }
            } catch (Throwable ex) {
                log4j.error("Oops. Error in Logger !", ex);
            }
        }
    }

    /**
     * Log with INFO level
     * @param e the exception to log
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void info(Throwable e, String message, Object... args) {
        if (forceJuli || log4j == null) {
            try {
                if (!niceThrowable(Priority.INFO, e, message, args)) {
                    juli.log(Level.INFO, format(message, args), e);
                }
            } catch (Throwable ex) {
                juli.log(Level.SEVERE, "Oops. Error in Logger !", ex);
            }
        } else {
            try {
                if (!niceThrowable(Priority.INFO, e, message, args)) {
                    if (recordCaller) {
                        CallInfo ci = getCallerInformations(3);
                        log4j.getLogger(ci.className).info(format(message, args), e);
                    } else {
                        log4j.info(format(message, args), e);
                    }
                }
            } catch (Throwable ex) {
                log4j.error("Oops. Error in Logger !", ex);
            }
        }
    }

    /**
     * Log with WARN level
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void warn(String message, Object... args) {
        if (forceJuli || log4j == null) {
            try {
                juli.warning(format(message, args));
            } catch (Throwable ex) {
                juli.log(Level.SEVERE, "Oops. Error in Logger !", ex);
            }
        } else {
            try {
                if (recordCaller) {
                    CallInfo ci = getCallerInformations(3);
                    log4j.getLogger(ci.className).warn(format(message, args));
                } else {
                    log4j.warn(format(message, args));
                }
            } catch (Throwable ex) {
                log4j.error("Oops. Error in Logger !", ex);
            }
        }
    }

    /**
     * Log with WARN level
     * @param e the exception to log
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void warn(Throwable e, String message, Object... args) {
        if (forceJuli || log4j == null) {
            try {
                if (!niceThrowable(Priority.WARN, e, message, args)) {
                    juli.log(Level.WARNING, format(message, args), e);
                }
            } catch (Throwable ex) {
                juli.log(Level.SEVERE, "Oops. Error in Logger !", ex);
            }
        } else {
            try {
                if (!niceThrowable(Priority.WARN, e, message, args)) {
                    if (recordCaller) {
                        CallInfo ci = getCallerInformations(3);
                        log4j.getLogger(ci.className).warn(format(message, args), e);
                    } else {
                        log4j.warn(format(message, args), e);
                    }
                }
            } catch (Throwable ex) {
                log4j.error("Oops. Error in Logger !", ex);
            }
        }
    }

    /**
     * Log with ERROR level
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void error(String message, Object... args) {
        if (forceJuli || log4j == null) {
            try {
                juli.severe(format(message, args));
            } catch (Throwable ex) {
                juli.log(Level.SEVERE, "Oops. Error in Logger !", ex);
            }
        } else {
            try {
                if (recordCaller) {
                    CallInfo ci = getCallerInformations(3);
                    log4j.getLogger(ci.className).error(format(message, args));
                } else {
                    log4j.error(format(message, args));
                }
            } catch (Throwable ex) {
                log4j.error("Oops. Error in Logger !", ex);
            }
        }
    }

    /**
     * Log with ERROR level
     * @param e the exception to log
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void error(Throwable e, String message, Object... args) {
        if (forceJuli || log4j == null) {
            try {
                if (!niceThrowable(Priority.ERROR, e, message, args)) {
                    juli.log(Level.SEVERE, format(message, args), e);
                }
            } catch (Throwable ex) {
                juli.log(Level.SEVERE, "Oops. Error in Logger !", ex);
            }
        } else {
            try {
                if (!niceThrowable(Priority.ERROR, e, message, args)) {
                    if (recordCaller) {
                        CallInfo ci = getCallerInformations(3);
                        log4j.getLogger(ci.className).error(format(message, args), e);
                    } else {
                        log4j.error(format(message, args), e);
                    }
                }
            } catch (Throwable ex) {
                log4j.error("Oops. Error in Logger !", ex);
            }
        }
    }

    /**
     * Log with FATAL level
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void fatal(String message, Object... args) {
        if (forceJuli || log4j == null) {
            try {
                juli.severe(format(message, args));
            } catch (Throwable ex) {
                juli.log(Level.SEVERE, "Oops. Error in Logger !", ex);
            }
        } else {
            try {
                if (recordCaller) {
                    CallInfo ci = getCallerInformations(3);
                    log4j.getLogger(ci.className).fatal(format(message, args));
                } else {
                    log4j.fatal(format(message, args));
                }
            } catch (Throwable ex) {
                log4j.error("Oops. Error in Logger !", ex);
            }
        }
    }

    /**
     * Log with FATAL level
     * @param e the exception to log
     * @param message The message pattern
     * @param args Pattern arguments
     */
    public static void fatal(Throwable e, String message, Object... args) {
        if (forceJuli || log4j == null) {
            try {
                if (!niceThrowable(Priority.FATAL, e, message, args)) {
                    juli.log(Level.SEVERE, format(message, args), e);
                }
            } catch (Throwable ex) {
                juli.log(Level.SEVERE, "Oops. Error in Logger !", ex);
            }
        } else {
            try {
                if (!niceThrowable(Priority.FATAL, e, message, args)) {
                    if (recordCaller) {
                        CallInfo ci = getCallerInformations(3);
                        log4j.getLogger(ci.className).fatal(format(message, args), e);
                    } else {
                        log4j.fatal(format(message, args), e);
                    }
                }
            } catch (Throwable ex) {
                log4j.error("Oops. Error in Logger !", ex);
            }
        }
    }
    // If e is a PlayException -> a very clean report
    static boolean niceThrowable(Priority priority, Throwable e, String message, Object... args) {
        if (e instanceof PlayException) {

            Throwable toClean = e;
            for (int i = 0; i < 5; i++) {
                // Clean stack trace
                List<StackTraceElement> cleanTrace = new ArrayList<StackTraceElement>();
                for (StackTraceElement se : toClean.getStackTrace()) {
                    if (se.getClassName().startsWith("org.apache.mina.")) {
                        cleanTrace.add(new StackTraceElement("Play!", "HTTP Server", "Mina", -1));
                        break;
                    }
                    cleanTrace.add(se);
                }
                toClean.setStackTrace(cleanTrace.toArray(new StackTraceElement[cleanTrace.size()]));
                toClean = toClean.getCause();
                if (toClean == null) {
                    break;
                }
            }

            PlayException playException = (PlayException) e;
            StringWriter sw = new StringWriter();
            PrintWriter errorOut = new PrintWriter(sw);


            errorOut.println("");
            errorOut.println("");
            errorOut.println("@" + playException.getId());
            errorOut.println("For request " + Request.current());
            errorOut.println(format(message, args));
            errorOut.println("");
            if (playException.isSourceAvailable()) {
                errorOut.println(playException.getErrorTitle() + " (In " + playException.getSourceFile() + " around line " + playException.getLineNumber() + ")");

            } else {
                errorOut.println(playException.getErrorTitle());
            }
            errorOut.println(playException.getErrorDescription().replace("<strong>", "").replace("</strong>", "").replace("\n", " "));
            if (forceJuli || log4j == null) {
                juli.log(toJuliLevel(priority.toString()), sw.toString(), e);
            } else {
                log4j.log(priority, sw.toString(), e);
            }
            return true;
        }
        return false;
    }

    static String format(String msg, Object... args) {
        try {
            if (args != null && args.length > 0) {
                return String.format(msg, args);
            }
            return msg;
        } catch(Exception e) {
            return msg;
        }
    }

    static class CallInfo {

        public String className;
        public String methodName;

        public CallInfo() {
        }

        public CallInfo(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

    }

    /**
     * Examine stack trace to get caller
     * @param level method stack depth
     * @return who called the logger
     */
    public static CallInfo getCallerInformations(int level) {
        StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
        StackTraceElement caller = callStack[level];
        return new CallInfo(caller.getClassName(), caller.getMethodName());
    }

    /**
     * Redirect java.util.logging to log4j
     */
    public static class JuliToLog4jHandler extends Handler {

        public void publish(LogRecord record) {
            org.apache.log4j.Logger log4j = getTargetLogger(record.getLoggerName());
            Priority priority = toLog4j(record.getLevel());
            log4j.log(priority, toLog4jMessage(record), record.getThrown());
        }

        static org.apache.log4j.Logger getTargetLogger(String loggerName) {
            return org.apache.log4j.Logger.getLogger(loggerName);
        }

        public static org.apache.log4j.Logger getTargetLogger(Class clazz) {
            return getTargetLogger(clazz.getName());
        }

        private String toLog4jMessage(LogRecord record) {
            String message = record.getMessage();
            // Format message
            try {
                Object parameters[] = record.getParameters();
                if (parameters != null && parameters.length != 0) {
                    // Check for the first few parameters ?
                    if (message.indexOf("{0}") >= 0 ||
                            message.indexOf("{1}") >= 0 ||
                            message.indexOf("{2}") >= 0 ||
                            message.indexOf("{3}") >= 0) {
                        message = MessageFormat.format(message, parameters);
                    }
                }
            } catch (Exception ex) {
                // ignore Exception
            }
            return message;
        }

        private org.apache.log4j.Level toLog4j(java.util.logging.Level level) {
            if (java.util.logging.Level.SEVERE == level) {
                return org.apache.log4j.Level.ERROR;
            } else if (java.util.logging.Level.WARNING == level) {
                return org.apache.log4j.Level.WARN;
            } else if (java.util.logging.Level.INFO == level) {
                return org.apache.log4j.Level.INFO;
            } else if (java.util.logging.Level.OFF == level) {
                return org.apache.log4j.Level.TRACE;
            }
            return org.apache.log4j.Level.TRACE;
        }

        @Override
        public void flush() {
            // nothing to do
        }

        @Override
        public void close() {
            // nothing to do
        }
    }
}
