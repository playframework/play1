package play.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helpers {

    static Pattern pattern = Pattern.compile("^(\\w+)\\(\\s*(?:('(?:\\\\'|[^'])*'|[^.]+?)\\s*(?:,\\s*('(?:\\\\'|[^'])*'|[^.]+?)\\s*)?)?\\)$");

    public static String[] seleniumCommand(String command) {
        Matcher matcher = pattern.matcher(command.trim());
        if (matcher.matches()) {
            String[] result = new String[3];
            result[0] = matcher.group(1);
            result[1] = matcher.group(2)!=null?matcher.group(2):"";
            result[2] = matcher.group(3)!=null?matcher.group(3):"";
            for (int i = 0; i < result.length; i++) {
                if (result[i].matches("^'.*'$")) {
                    result[i] = result[i].substring(1, result[i].length() - 1);
                    result[i] = result[i].replace("\\'", "'");
                }
            }
            return result;
        } else {
            return null;
        }
    }
}
