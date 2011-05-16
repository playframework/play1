package play.utils;

import java.util.MissingFormatArgumentException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SafeFormatter {
	// Pattern copied from java.util.Formatter
    private static final Pattern formatSpecifier
	= Pattern.compile("%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])");
 
    public String format(String format, Object...args) {
		Matcher matcher = formatSpecifier.matcher(format);
    	StringBuffer sb = new StringBuffer();
    	int lastAppend = 0;
    	int index = 0;
    	int regularIndex = 0;
		while(matcher.find()) {
			String indexString = matcher.group(1);
			String flags = matcher.group(2);
			if(indexString != null) {
				index = Integer.parseInt(indexString.substring(0, indexString.length() - 1));
			} else if(flags != null && flags.contains("<")) {
				flags = flags.replaceAll("\\<", "");
				// Relative index, leave index at previous value
			} else {
				// Regular index
				index = ++regularIndex;
			}
			
			StringBuilder newFormatPattern = new StringBuilder(matcher.end() - matcher.start());
			newFormatPattern.append("%");
			newFormatPattern.append(flags);
			for (int i = 3; i <= matcher.groupCount(); i++) {
				String group = matcher.group(i);
				if(group != null) {
					newFormatPattern.append(group);
				}
			}
			
			sb.append(append(format.substring(lastAppend, matcher.start())));

			if(matcher.group(6) != null && (matcher.group(6).equals("n") || matcher.group(6).equals("%"))) {
				//Parameters that don't require an argument
				sb.append(appendArgument(newFormatPattern.toString(), null));
			} else {
				//Parameters that do require an argument
			    if (args != null && index > args.length)
					throw new MissingFormatArgumentException(matcher.group());
				    
				sb.append(appendArgument(newFormatPattern.toString(), args == null ? null : args[index - 1]));
			}
			
			lastAppend = matcher.end();
		}
		
		sb.append(append(format.substring(lastAppend, format.length())));
		
		return sb.toString();
    }
    
	public abstract String appendArgument(String format, Object arg);
	public abstract String append(String value);
}
