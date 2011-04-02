package play.exceptions;

/**
 * Error in application.conf
 */
public class ConfigurationException extends PlayException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorDescription() {
        return getMessage();
    }

    @Override
    public String getErrorTitle() {
        return "Configuration error.";
    }
    
}
