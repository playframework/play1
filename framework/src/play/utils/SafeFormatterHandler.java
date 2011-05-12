package play.utils;

public interface SafeFormatterHandler {
	public String appendArgument(String format, Object arg);
	public String append(String value);
}
