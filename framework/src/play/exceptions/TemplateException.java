package play.exceptions;

import java.util.Arrays;
import java.util.List;
import play.templates.Template;

/**
 * An exception during template execution
 */
public abstract class TemplateException extends PlayException implements SourceAttachment {

    private final Template template;
    private final Integer lineNumber;

    public TemplateException(Template template, Integer lineNumber, String message) {
        super(message);
        this.template = template;
        this.lineNumber = lineNumber;
    }

    public TemplateException(Template template, Integer lineNumber, String message, Throwable cause) {
        super(message, cause);
        this.template = template;
        this.lineNumber = lineNumber;
    }

    public Template getTemplate() {
        return template;
    }

    @Override
    public Integer getLineNumber() {
        return lineNumber;
    }

    @Override
    public List<String> getSource() {
        return Arrays.asList(template.source.split("\n"));
    }

    @Override
    public String getSourceFile() {
        return template.name;
    }
}
