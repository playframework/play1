package play.exceptions;

/**
 * A exception during tag invocation
 */
public class TagInternalException extends RuntimeException {
    
	private static final long serialVersionUID = 1999029734850127428L;

	public TagInternalException(String message) {
        super(message);
    }

}
