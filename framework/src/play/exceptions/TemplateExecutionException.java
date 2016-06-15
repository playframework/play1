package play.exceptions;

import play.templates.Template;

/**
 * An exception during template execution
 */
public class TemplateExecutionException extends TemplateException {

    public TemplateExecutionException(Template template, Integer lineNumber, String message, Throwable cause) {
        super(template, lineNumber, message, cause);
    }

    @Override
    public String getErrorTitle() {
        return "Template execution error";
    }

    @Override
    public String getErrorDescription() {
        return String.format("Execution error occurred in template <strong>%s</strong>. Exception raised was <strong>%s</strong> : <strong>%s</strong>.", getSourceFile(), getCause().getClass().getSimpleName(), getMessage());
    }
    
    public static class DoBodyException extends RuntimeException {
        public DoBodyException(Throwable cause) {
            super(cause);
        }
    }
}

