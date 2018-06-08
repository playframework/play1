package play.exceptions;

public class RestartNeededException extends Exception {
    public RestartNeededException(String message) {
        super(message);
    }

    public RestartNeededException(String message, Throwable cause) {
        super(message, cause);
    }
}
