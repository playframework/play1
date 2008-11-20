package play.templates;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang.StringEscapeUtils;

import play.i18n.Lang;
import play.libs.I18N;

public class JavaExtensions {

    public static String capitalizeWords(String source) {
        char prevc = ' '; // first char of source is capitalized
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c != ' ' && prevc == ' ') {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(c);
            }
            prevc = c;
        }
        return sb.toString();
    }

    public static String escapeHtml(String htmlToEscape) {
        return StringEscapeUtils.escapeHtml(htmlToEscape);
    }

    public static String escapeJavaScript(String str) {
        return StringEscapeUtils.escapeJavaScript(str);
    }

    public static String escapeXml(String str) {
        return StringEscapeUtils.escapeXml(str);
    }

    public static String format(Number number, String pattern) {
        return new DecimalFormat(pattern).format(number);
    }

    public static String format(Date date, String pattern) {
        return new SimpleDateFormat(pattern).format(date);
    }

    public static String asdate(Long timestamp, String pattern) {
        return new SimpleDateFormat(pattern).format(new Date(timestamp));
    }

    public static String nl2br(String data) {
        return data.replace("\n", "<br/>");
    }

    public static String formatSize(Long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        if (bytes < 1048576L) {
            return bytes / 1024L + "KB";
        }
        if (bytes < 1073741824L) {
            return bytes / 1048576L + "." + "MB";
        }
        return bytes / 1073741824L + "GB";
    }

    public static String formatCurrency(Number number, String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(new Locale(Lang.get()));
        numberFormat.setCurrency(currency);
        numberFormat.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        String s = numberFormat.format(number);
        s = s.replace(currencyCode, I18N.getCurrencySymbol(currencyCode));
        return s;
    }

    public static String addSlashes(Object o) {
        String string = o.toString();
        return string.replace("\"", "\\\"").replace("'", "\\'");
    }

    public static String capFirst(Object o) {
        String string = o.toString();
        if (string.length() == 0) {
            return string;
        }
        return ("" + string.charAt(0)).toUpperCase() + string.substring(1);
    }
    
    public static String capAll(Object o) {
        String string = o.toString();        
        return capitalizeWords(string);
    }

    public static String cut(Object o, String pattern) {
        String string = o.toString();
        return string.replace(pattern, "");
    }

    public static boolean divisibleBy(Number n, int by) {
        return n.longValue() % by == 0;
    }

    public static String escape(Object o) {
        String string = o.toString();
        return escapeHtml(string);
    }

    public static String pluralize(Number n) {
        long l = n.longValue();
        if (l > 1) {
            return "s";
        }
        return "";
    }

    public static String pluralize(Number n, String plural) {
        long l = n.longValue();
        if (l > 1) {
            return plural;
        }
        return "";
    }

    public static String pluralize(Number n, String[] forms) {
        long l = n.longValue();
        if (l > 1) {
            return forms[1];
        }
        return forms[0];
    }
}
