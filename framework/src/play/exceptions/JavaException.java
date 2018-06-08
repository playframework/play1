package play.exceptions;

import play.classloading.ApplicationClasses.ApplicationClass;

abstract class JavaException extends PlayExceptionWithJavaSource {

    JavaException(ApplicationClass applicationClass, Integer line, String message, Throwable cause) {
        super(message, cause, applicationClass, line);
    }

    @Override
    public boolean isSourceAvailable() {
        return super.isSourceAvailable() && line != null;
    }
}
