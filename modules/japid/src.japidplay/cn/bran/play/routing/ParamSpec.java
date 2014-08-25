/**
 * 
 */
package cn.bran.play.routing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The format of the expression is: "{" variable-name [ ":" regular-expression ]
 * "}" The regular-expression part is optional. When the expression is not
 * provided, it defaults to a wildcard matching of one particular segment. In
 * regular-expression terms, the expression defaults to "([]*)"
 * 
 * @author bran
 * 
 */
public class ParamSpec {
	Pattern formatPattern;
	String name;
	String format = "[^/]+"; // the default regex
	Class<?> type;
	static final String varNamePatternText = "[a-zA-Z_$][a-zA-Z_$0-9]*";
	static final String paramSpecPatternText = "(<(.+)>)?" + "(" + varNamePatternText + ")";
	static final Pattern paramSpecPattern = Pattern.compile(paramSpecPatternText);

	/**
	 * @param s
	 */
	public ParamSpec(String s) {
		String[] ex = extract(s);
		name = ex[0];
		format = ex[1];
		formatPattern = Pattern.compile(format);
	}

	public static String[] extract(String s) {
		Matcher matcher = paramSpecPattern.matcher(s);
		if (matcher.find()) {
			String form = matcher.group(2);
			form = form == null ? "" : form;
			String var = matcher.group(3);
			return new String[] { form, var };
		}
		throw new RuntimeException("param spec does not match the pattern: " + paramSpecPatternText
				+ ". The input is: " + s);
	}
}