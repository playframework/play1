package play.exceptions;

/**
 * HTTP to Java Binding error
 */
public class BindingException extends PlayException {
    
    public BindingException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorTitle() {
        return "Binding exception";
    }

    @Override
    public String getErrorDescription() {
        return "TODO";
    }

}
