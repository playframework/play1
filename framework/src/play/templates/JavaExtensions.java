package play.templates;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class JavaExtensions {

    public static String escape(String htmlToEscape) {
        return htmlToEscape.replace("<", "&lt;").replace(">", "&gt;");
    }

    public static String format(Number number, String pattern) {
        return new DecimalFormat(pattern).format(number);
    }
    
    public static String format(Date date, String pattern) {
        return new SimpleDateFormat(pattern).format(date);
    }
}
