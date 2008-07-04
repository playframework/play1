package play.templates;

import java.text.DecimalFormat;

public class JavaExtensions {

    public static String escape(String htmlToEscape) {
        return htmlToEscape.replace("<", "&lt;").replace(">", "&gt;");
    }

    public static String format(Number number, String pattern) {
        return new DecimalFormat(pattern).format(number);
    }
}
