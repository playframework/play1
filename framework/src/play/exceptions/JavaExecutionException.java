package play.exceptions;

import play.classloading.ApplicationClasses.ApplicationClass;

public class JavaExecutionException extends JavaException {

    public JavaExecutionException(ApplicationClass applicationClass, Integer lineNumber, Throwable e) {
        super(applicationClass, lineNumber, e.getMessage(), e);
    }
    
    public JavaExecutionException(Throwable e) {
        super(null, null, e.getMessage(), e);
    }

    @Override
    public String getErrorTitle() {
        return "Execution exception";
    }

    @Override
    public String getErrorDescription() {
        return String.format("<strong>%s</strong> occurred : %s", getCause().getClass().getSimpleName(), getMessage());
    } 
}

