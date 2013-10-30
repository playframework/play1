package play.exceptions;

public class BinderException extends UnexpectedException {

    public BinderException(String message) {
        super(message);
    }

    public BinderException(Throwable exception) {
        super(exception);
    }

    public BinderException(String message, Throwable cause) {
        super(message, cause);
    }

}