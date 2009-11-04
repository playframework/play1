package play.exceptions;

import play.templates.Template;

/**
 * An exception during template execution
 */
public class TemplateExecutionException extends TemplateException {

	private static final long serialVersionUID = -2382896036549635081L;

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
		private static final long serialVersionUID = -1748197998247142585L;

		public DoBodyException(Throwable cause) {
            super(cause);
        }
    }
}

