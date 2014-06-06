package play.exceptions;

/**
 * Cache related exception
 */
public class CacheException extends PlayExceptionWithJavaSource {
    
    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorTitle() {
        return "Cache error";
    } 

    @Override
    public String getErrorDescription() {
        return getMessage();
    }
}
