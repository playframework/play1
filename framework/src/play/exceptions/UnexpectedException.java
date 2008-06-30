package play.exceptions;

public class UnexpectedException extends PlayException {
    
    public UnexpectedException(String message) {
        super(message);
    }

    public UnexpectedException(Throwable exception) {
        super("Unexpected Error", exception);
    }
    
    public UnexpectedException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorTitle() {
        if(getCause() == null) {
            return "Unexpected error";
        }
        return String.format("Unexpected error: %s", getCause().getClass().getSimpleName());
    }

    @Override
    public String getErrorDescription() {
        return String.format("An unexpected error occured caused by exception <strong>%s</strong>:<br/> <strong>%s</strong>", getCause().getClass().getSimpleName(), getCause().getMessage());
    }
}

