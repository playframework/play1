package play.exceptions;

public class BindingException extends PlayException {
    
    public BindingException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorTitle() {
        return "TODO";
    }

    @Override
    public String getErrorDescription() {
        return "TODO";
    }

}
