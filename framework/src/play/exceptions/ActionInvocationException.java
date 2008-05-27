package play.exceptions;

import play.classloading.ApplicationClasses.ApplicationClass;

public class ActionInvocationException extends JavaException {

    String action;

    public ActionInvocationException(ApplicationClass applicationClass, String action, Integer lineNumber, Throwable e) {
        super(applicationClass, lineNumber, e.getMessage(), e);
        this.action = action;
    }
    
    public ActionInvocationException(String action, Throwable e) {
        super(null, null, e.getMessage(), e);
        this.action = action;
    }

    @Override
    public String getErrorTitle() {
        return String.format("Invocation error",  action);
    }

    @Override
    public String getErrorDescription() {
        return String.format("An exception <strong>%s</strong> occured in action <strong>%s</strong> : %s", getCause().getClass().getSimpleName(), action, getMessage());
    }
}

