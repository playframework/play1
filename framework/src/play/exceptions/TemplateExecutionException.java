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
        return String.format("Execution error");
    }

    @Override
    public String getErrorDescription() {
        return  String.format("Execution error occured in page %s. Source file <strong>%s</strong> could not be parsed\nAn exception <strong>%s</strong> was raised : <strong>%s</strong>", getTemplate(), getSourceFile(), getCause().getClass().getSimpleName(), getMessage());
    }
    
    public static class DoBodyException extends RuntimeException {
        public DoBodyException(Exception cause) {
            super(cause);
        }
    }
}

