package play.exceptions;

import play.classloading.ApplicationClasses.ApplicationClass;

/**
 * A Java exception
 */
public abstract class JavaException extends PlayExceptionWithJavaSource {

    public JavaException(ApplicationClass applicationClass, Integer line, String message) {
        super(message, null, applicationClass, line);
    }

    public JavaException(ApplicationClass applicationClass, Integer line, String message, Throwable cause) {
        super(message, cause, applicationClass, line);
    }

    @Override
    public boolean isSourceAvailable() {
        return super.isSourceAvailable() && line != null;
    }
}
