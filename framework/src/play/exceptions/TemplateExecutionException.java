package play.exceptions;

import play.templates.Template;

public class TemplateExecutionException extends TemplateException {

    public TemplateExecutionException(Template template, Integer lineNumber, String message) {
        super(template, lineNumber, message);
    }

    public TemplateExecutionException(Template template, Integer lineNumber, String message, Throwable cause) {
        super(template, lineNumber, message, cause);
    }

    @Override
    public String getErrorTitle() {
        return String.format("Template execution error");
    }

    @Override
    public String getErrorDescription() {
        return  String.format("Execution error occured in template <strong>%s</strong>. Exception raised was <strong>%s</strong> : <strong>%s</strong>.", getSourceFile(), getCause().getClass().getSimpleName(), getMessage());
    }
    
    public static class DoBodyException extends RuntimeException {
        public DoBodyException(Exception cause) {
            super(cause);
        }
    }
}

