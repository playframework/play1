package play.exceptions;

import play.Play;

public class ActionInvocationException extends JavaException {

    private String action;
    private String className;

    public ActionInvocationException(String action, String fileName, Integer lineNumber, Throwable e) {
        super(fileName, lineNumber, e.getMessage(), e);
        this.action = action;
    }

    public static PlayException toActionInvocationException(String action, Throwable cause) {
        for (StackTraceElement stackTraceElement : cause.getStackTrace()) {
            if (stackTraceElement.getLineNumber() > 0 && Play.classes.hasClass(stackTraceElement.getClassName())) {
                String className = stackTraceElement.getClassName();
                String fileName = stackTraceElement.getFileName();
                if (className.contains(".")) {
                    String packageName = className.substring(0, className.lastIndexOf("."));
                    fileName = packageName.replace(".", "/") + "/" + fileName;
                }
                return new ActionInvocationException(action, fileName, stackTraceElement.getLineNumber(), cause);
            }
        }
        return new UnexpectedException(cause);
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String getErrorTitle() {
        return String.format("Invocation error on action %s",  action);
    }

    @Override
    public String getErrorDescription() {
        return String.format("An exception <strong>%s</strong> was raised in class <strong>%s</strong> : <strong>%s</strong>", getCause().getClass().getSimpleName(), className, getMessage());
    }
}

