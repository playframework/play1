package play.exceptions;

import play.Play;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static play.classloading.ApplicationClasses.ApplicationClass;

public abstract class PlayExceptionWithJavaSource extends PlayException implements SourceAttachment {
    ApplicationClass applicationClass;
    Integer line;

    protected PlayExceptionWithJavaSource(String message) {
        super(message);
    }

    protected PlayExceptionWithJavaSource(String message, Throwable cause, ApplicationClass applicationClass, Integer line) {
        super(message, cause);
        this.applicationClass = applicationClass;
        this.line = line;
    }

    protected PlayExceptionWithJavaSource(String message, Throwable cause) {
        super(message, cause);

        StackTraceElement element = getInterestingStackTraceElement(cause);
        if (element != null) {
            applicationClass = Play.classes.getApplicationClass(element.getClassName());
            line = element.getLineNumber();
        }
    }

    @Override
    public String getSourceFile() {
        return isSourceAvailable() && applicationClass.javaFile != null ? applicationClass.javaFile.relativePath() : null;
    }

    @Override
    public List<String> getSource() {
        return isSourceAvailable() ? asList(applicationClass.javaSource.split("\n")) : Collections.<String>emptyList();
    }

    @Override
    public Integer getLineNumber() {
        return line;
    }

    @Override
    public boolean isSourceAvailable() {
        return applicationClass != null && applicationClass.javaSource != null;
    }
}
