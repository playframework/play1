package play.exceptions;

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
}