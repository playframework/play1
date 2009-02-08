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
import play.i18n.Messages;
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
           return Messages.get("since.seconds", delta);
       }
       if(delta < 60 * 60) {
           return Messages.get("since.minutes", delta / 60);
       }
       if(delta < 24 * 60 * 60) {
           return Messages.get("since.hours", delta / (60 * 60) );
       }
       if(delta < 30 * 24 * 60 * 60) {
           return Messages.get("since.days", delta / (24 * 60 * 60) );
       }
       if(delta < 365 * 24 * 60 * 60) {
           return Messages.get("since.months", delta / (30 * 24 * 60 * 60) );
       }
       return Messages.get("since.years", delta / (365 * 24 * 60 * 60) );
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
        return string.replaceAll("[àáâãäåāąă]", "a")
		.replaceAll("[çćčĉċ]", "c")
		.replaceAll("[ďđð]", "d")
		.replaceAll("[èéêëēęěĕė]", "e")
		.replaceAll("[ƒſ]", "f")
		.replaceAll("[ĝğġģ]", "g")
		.replaceAll("[ĥħ]", "h")
		.replaceAll("[ìíîïīĩĭįı]", "i")
		.replaceAll("[ĳĵ]", "j")
		.replaceAll("[ķĸ]", "k")
		.replaceAll("[łľĺļŀ]", "l")
		.replaceAll("[ñńňņŉŋ]", "n")
		.replaceAll("[òóôõöøōőŏœ]", "o")
		.replaceAll("[Þþ]", "p")
		.replaceAll("[ŕřŗ]", "r")
		.replaceAll("[śšşŝș]", "s")
		.replaceAll("[ťţŧț]", "t")
		.replaceAll("[ùúûüūůűŭũų]", "u")
		.replaceAll("[ŵ]", "w")
		.replaceAll("[ýÿŷ]", "y")
		.replaceAll("[žżź]", "z")
        .replaceAll("[æ]", "ae")
		.replaceAll("[ÀÁÂÃÄÅĀĄĂ]", "A")
		.replaceAll("[ÇĆČĈĊ]", "C")
		.replaceAll("[ĎĐÐ]", "D")
		.replaceAll("[ÈÉÊËĒĘĚĔĖ]", "E")
		.replaceAll("[ĜĞĠĢ]", "G")
		.replaceAll("[ĤĦ]", "H")
		.replaceAll("[ÌÍÎÏĪĨĬĮİ]", "I")
		.replaceAll("[Ĵ]", "J")
		.replaceAll("[Ķ]", "K")
		.replaceAll("[ŁĽĹĻĿ]", "L")
		.replaceAll("[ÑŃŇŅŊ]", "N")
		.replaceAll("[ÒÓÔÕÖØŌŐŎ]", "O")
		.replaceAll("[ŔŘŖ]", "R")
		.replaceAll("[ŚŠŞŜȘ]", "S")
		.replaceAll("[ÙÚÛÜŪŮŰŬŨŲ]", "U")
		.replaceAll("[Ŵ]", "W")
		.replaceAll("[ÝŶŸ]", "Y")
		.replaceAll("[ŹŽŻ]", "Z")
        .replaceAll("[ß]", "ss");
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
