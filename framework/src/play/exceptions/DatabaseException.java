
package play.exceptions;

import java.util.Arrays;
import java.util.List;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;

/**
 * Database error
 */
public class DatabaseException extends PlayException implements SourceAttachment {
    
    String sourceFile;
    List<String> source;
    Integer line;    
    
    public DatabaseException(String message) {
        super(message, null);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
        StackTraceElement element = getInterestingStrackTraceElement(cause);
        if(element != null) {
            ApplicationClass applicationClass = Play.classes.getApplicationClass(element.getClassName());
            sourceFile = applicationClass.javaFile.relativePath();
            source = Arrays.asList(applicationClass.javaSource.split("\n"));
            line = element.getLineNumber();
        }
    }

    @Override
    public String getErrorTitle() {
        return "Database error";
    }

    @Override
    public String getErrorDescription() {
        return String.format("A database error occured : <strong>%s</strong>", getMessage());
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public List<String> getSource() {
        return source;
    }

    public Integer getLineNumber() {
        return line;
    }

    @Override
    public boolean isSourceAvailable() {
        return sourceFile != null;
    }

}
