package play.utils;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Colour-coded console appender for Log4J.
 */
public class ANSIConsoleAppender extends ConsoleAppender {

    static final int NORMAL = 0;
    static final int BRIGHT = 1;
    static final int FOREGROUND_BLACK = 30;
    static final int FOREGROUND_RED = 31;
    static final int FOREGROUND_GREEN = 32;
    static final int FOREGROUND_YELLOW = 33;
    static final int FOREGROUND_BLUE = 34;
    static final int FOREGROUND_MAGENTA = 35;
    static final int FOREGROUND_CYAN = 36;
    static final int FOREGROUND_WHITE = 37;
    static final String PREFIX = "\u001b[";
    static final String SUFFIX = "m";
    static final char SEPARATOR = ';';
    static final String END_COLOUR = PREFIX + SUFFIX;
    static final String FATAL_COLOUR = PREFIX + BRIGHT + SEPARATOR + FOREGROUND_RED + SUFFIX;
    static final String ERROR_COLOUR = PREFIX + NORMAL + SEPARATOR + FOREGROUND_RED + SUFFIX;
    static final String WARN_COLOUR = PREFIX + NORMAL + SEPARATOR + FOREGROUND_YELLOW + SUFFIX;
    static final String INFO_COLOUR = PREFIX + SUFFIX;
    static final String DEBUG_COLOUR = PREFIX + NORMAL + SEPARATOR + FOREGROUND_CYAN + SUFFIX;
    static final String TRACE_COLOUR = PREFIX + NORMAL + SEPARATOR + FOREGROUND_BLUE + SUFFIX;

    /**
     * Wraps the ANSI control characters around the
     * output from the super-class Appender.
     */
    @Override
    protected void subAppend(LoggingEvent event) {
        this.qw.write(getColour(event.getLevel()));
        super.subAppend(event);
        this.qw.write(END_COLOUR);
        if (this.immediateFlush) {
            this.qw.flush();
        }
    }

    /**
     * Get the appropriate control characters to change
     * the colour for the specified logging level.
     */
    private String getColour(org.apache.log4j.Level level) {
        switch (level.toInt()) {
            case Priority.FATAL_INT:
                return FATAL_COLOUR;
            case Priority.ERROR_INT:
                return ERROR_COLOUR;
            case Priority.WARN_INT:
                return WARN_COLOUR;
            case Priority.INFO_INT:
                return INFO_COLOUR;
            case Priority.DEBUG_INT:
                return DEBUG_COLOUR;
            default:
                return TRACE_COLOUR;
        }
    }
}
