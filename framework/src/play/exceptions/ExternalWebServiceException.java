package play.exceptions;

public class WebServiceException extends PlayException {

    public WebServiceException(String message) {
        super(message);
    }

    public WebServiceException(Throwable cause) {
        super("An exception occured using external web service.", cause);
    }

    public WebServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
	public String getErrorTitle() {
        return "An exception occured using external web service.";
    }

    @Override
    public String getExceptionDescription() {
        if (getCause == null) {
            return getErrorTitle();
        } else {
            return String.format("An exception occurred using external web service: %s", getCause().getClass().getSimpleName());
        }
    }
}