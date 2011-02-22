package play.exceptions;

public class ExternalWebServiceException extends PlayException {

    public ExternalWebServiceException(String message) {
        super(message);
    }

    public ExternalWebServiceException(Throwable cause) {
        super("An exception occured using external web service.", cause);
    }

    public ExternalWebServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
	public String getErrorTitle() {
        return "An exception occured using external web service.";
    }

    @Override
    public String getErrorDescription() {
        if (getCause() == null) {
            return getErrorTitle();
        } else {
            return String.format("An exception occurred using external web service: %s", getCause().getClass().getSimpleName());
        }
    }
}