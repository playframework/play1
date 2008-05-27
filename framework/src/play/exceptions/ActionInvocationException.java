package play.exceptions;

import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;

public class ActionInvocationException extends JavaException {

    String action;

    public ActionInvocationException(ApplicationClass applicationClass, String action, Integer lineNumber, Throwable e) {
        super(applicationClass, lineNumber, e.getMessage(), e);
        this.action = action;
    }

    public static PlayException toActionInvocationException(String action, Throwable cause) {
        for (StackTraceElement stackTraceElement : cause.getStackTrace()) {
            if (stackTraceElement.getLineNumber() > 0 && Play.classes.hasClass(stackTraceElement.getClassName())) {
                String className = stackTraceElement.getClassName();                
                return new ActionInvocationException(Play.classes.getApplicationClass(className), action, stackTraceElement.getLineNumber(), cause);
            }
        }
        return new UnexpectedException(cause);
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

