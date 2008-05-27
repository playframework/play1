package play.exceptions;

public class ConfigurationException extends PlayException {

    public ConfigurationException(String message) {
        super(message);
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
