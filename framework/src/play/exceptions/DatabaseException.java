
package play.exceptions;

public class DatabaseException extends PlayExceptionWithJavaSource {
    
    public DatabaseException(String message) {
        super(message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorTitle() {
        return "Database error";
    }

    @Override
    public String getErrorDescription() {
        return String.format("A database error occurred : <strong>%s</strong>", getMessage());
    }
}
