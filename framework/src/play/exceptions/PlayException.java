package play.exceptions;

import play.Play;

public abstract class PlayException extends RuntimeException {

    public PlayException() {
        super();
    }

    public PlayException(String message) {
        super(message);
    }

    public PlayException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract String getErrorTitle();

    public abstract String getErrorDescription();

    public boolean isSourceAvailable() {
        return this instanceof SourceAttachment;
    }
    
    public static StackTraceElement getInterestingStrackTraceElement(Throwable cause) {
        for (StackTraceElement stackTraceElement : cause.getStackTrace()) {
            if (stackTraceElement.getLineNumber() > 0 && Play.classes.hasClass(stackTraceElement.getClassName())) {
                return stackTraceElement;             
            }
        }
        return null;
    }
    
}