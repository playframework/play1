package play.exceptions;

import play.classloading.ApplicationClasses.ApplicationClass;

/**
 * An exception occured during Java execution
 */
public class JavaExecutionException extends JavaException {

	private static final long serialVersionUID = 5094926773157916567L;

	public JavaExecutionException(ApplicationClass applicationClass, Integer lineNumber, Throwable e) {
        super(applicationClass, lineNumber, e.getMessage(), e);
    }
    
    public JavaExecutionException(String action, Throwable e) {
        super(null, null, e.getMessage(), e);
    }
    
    public JavaExecutionException(Throwable e) {
        super(null, null, e.getMessage(), e);
    }

    @Override
    public String getErrorTitle() {
        return String.format("Java exception");
    }

    @Override
    public String getErrorDescription() {
        return String.format("<strong>%s</strong> occured : %s", getCause().getClass().getSimpleName(), getMessage());
    } 
}

