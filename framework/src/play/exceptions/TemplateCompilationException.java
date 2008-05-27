package play.exceptions;

import play.templates.Template;


public class TemplateCompilationException extends TemplateException {

    public TemplateCompilationException(Template template, Integer lineNumber, String message) {
        super(template, lineNumber, message);
    }

    public TemplateCompilationException(Template template, Integer lineNumber, String message, Throwable cause) {
        super(template, lineNumber, message);
    }

    @Override
    public String getErrorTitle() {
        return String.format("Compilation error for template");
    }

    @Override
    public String getErrorDescription() {
        return String.format("The <strong>%s</strong> page does not compile at line %s.", getTemplate().name, getLineNumber());
    }
}
