package play.exceptions;

public class WebServiceException extends PlayException {

    public WebServiceException(String message) {
	super(message);
    }

    public WebServiceException(Throwable exception) {
	super("Error contacting web service.", exception);
    }

    public WebServiceException(String message, Throwable cause) {
	super(message, cause);
    }

    @Override
	public String getErrorTitle() {
	return "Web Service error";
    }

   @Override
       public String getErrorDescription() {
       return "Error using a web service.";
   }

}