package play.exceptions;

import play.Play;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The super class for all Play! exceptions
 */
public abstract class PlayException extends RuntimeException {

    private static AtomicLong atomicLong = new AtomicLong(System.currentTimeMillis());
    private String id;

    public PlayException() {
        setId();
    }

    public PlayException(String message) {
        super(message);
        setId();
    }

    public PlayException(String message, Throwable cause) {
        super(message, cause);
        setId();
    }

    void setId() {
        long nid = atomicLong.incrementAndGet();
        id = Long.toString(nid, 26);
    }

    public abstract String getErrorTitle();

    public abstract String getErrorDescription();

    public boolean isSourceAvailable() {
        return this instanceof SourceAttachment;
    }

    public Integer getLineNumber() {
        return -1;
    }

    public String getSourceFile() {
        return "";
    }

    public String getId() {
        return id;
    }

    @Deprecated
    public static StackTraceElement getInterestingStrackTraceElement(Throwable cause) {
      return getInterestingStackTraceElement(cause);
    }

    public static StackTraceElement getInterestingStackTraceElement(Throwable cause) {
        for (StackTraceElement stackTraceElement : cause.getStackTrace()) {
            if (stackTraceElement.getLineNumber() > 0 && Play.classes.hasClass(stackTraceElement.getClassName())) {
                return stackTraceElement;
            }
        }
        return null;
    }

    public String getMoreHTML() {
        return null;
    }
}