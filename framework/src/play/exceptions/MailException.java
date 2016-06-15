
package play.exceptions;

/**
 * Error while sending an email
 */
public class MailException extends PlayExceptionWithJavaSource {
    
    public MailException(String message) {
        super(message);
    }
    
    public MailException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorTitle() {
        return "Mail error";
    }

    @Override
    public String getErrorDescription() {
        return String.format("A mail error occurred : <strong>%s</strong>", getMessage());
    }
}
