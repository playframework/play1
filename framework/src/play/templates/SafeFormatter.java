package play.templates;

/**
 * Supported type for formatting. This interface is used to implement custom formatters for templates.
 * 
 */
public interface SafeFormatter {
	String format(Template template, Object value);
}
