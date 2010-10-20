package play.exceptions;

import java.util.Arrays;
import java.util.List;
import play.classloading.ApplicationClasses.ApplicationClass;

/**
 * A Java exception
 */
public abstract class JavaException extends PlayException implements SourceAttachment {

    private ApplicationClass applicationClass;
    private Integer lineNumber;

    public JavaException(ApplicationClass applicationClass, Integer lineNumber, String message) {
        super(message);
        this.applicationClass = applicationClass;
        this.lineNumber = lineNumber;
    }

    public JavaException(ApplicationClass applicationClass, Integer lineNumber, String message, Throwable cause) {
        super(message, cause);
        this.applicationClass = applicationClass;
        this.lineNumber = lineNumber;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public List<String> getSource() {
        return Arrays.asList(applicationClass.javaSource.split("\n"));
    }

    public String getSourceFile() {
        return applicationClass.javaFile.relativePath();
    }

    @Override
    public boolean isSourceAvailable() {
        return applicationClass != null && applicationClass.javaFile != null && lineNumber != null;
    }
}
