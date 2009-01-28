package play.templates;

import groovy.lang.Closure;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang.StringEscapeUtils;

import play.i18n.Lang;
import play.libs.I18N;

/**
 * Java extensions in templates
 */
public class JavaExtensions {
    
    public static boolean contains(String[] array, String value) {
        for(String v : array) {
            if(v.equals(value)) {
                return true;
            }
        }
        return false;
    }
    
    public static String toString(Closure closure) {
        PrintWriter oldWriter = (PrintWriter)closure.getProperty("out");
        StringWriter newWriter = new StringWriter();
        closure.setProperty("out", new PrintWriter(newWriter));
        closure.call();
        closure.setProperty("out", oldWriter);
        return newWriter.toString();
    }

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
    
    public static Integer page(Number number, Integer pageSize) {
        return number.intValue() / pageSize + (number.intValue() % pageSize > 0 ? 1 : 0);
    }
    
    public static String since(Date date) {
        Date now = new Date();
        if(now.before(date)) {
            return "";
        }
        long delta = (now.getTime() - date.getTime()) / 1000;
        if(delta < 60) {
            return String.format("%s seconds ago", delta);
        }
        if(delta < 60 * 60) {
            return String.format("%s minutes ago", delta / 60);
        }
        if(delta < 24 * 60 * 60) {
            return String.format("%s hours ago", delta / (60 * 60) );
        }
        if(delta < 30 * 24 * 60 * 60) {
            return String.format("%s days ago", delta / (24 * 60 * 60) );
        }
        return String.format("%s months ago", delta / (30 * 24 * 60 * 60) );
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
            return bytes / 1048576L + "MB";
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

    public static String noAccents(String string) {
        string = string.replaceAll("[éèêëæ]", "e");
        string = string.replaceAll("[àâä]", "a");
        string = string.replaceAll("[iïîì]", "i");
        string = string.replaceAll("[oôöò]", "o");
        string = string.replaceAll("[uûüù]", "u");
        string = string.replaceAll("[ñ]", "n");
        string = string.replaceAll("[ç]", "ç");
        string = string.replaceAll("[ÀÄÂÁ]", "A");
        string = string.replaceAll("[ËÊÈ]", "E");
        string = string.replaceAll("[ÎÏÌ]", "I");
        string = string.replaceAll("[ÛÙÜ]", "U");
        string = string.replaceAll("[ÔÖÒ]", "O");
        string = string.replaceAll("[Ñ]", "N");
        string = string.replaceAll("[Ç]", "C");
        return string;
    }

    public static String slugify(String string) {
        string = noAccents(string);
        return string.replaceAll("[^\\w]", "-").replaceAll("-{2,}", "-").replaceAll("-$", "").toLowerCase();
    }

    public static String camelCase(String string) {
        string = noAccents(string);
        string = string.replaceAll("[^\\w ]", "");
        StringBuilder result = new StringBuilder();
        for (String part : string.split(" ")) {
            result.append(capFirst(part));
        }
        return result.toString();
    }

    public static String yesno(Object o, String[] values) {
        boolean value = play.templates.FastTags._evaluateCondition(o);
        if (value) {
            return values[0];
        }
        return values[1];
    }
}
