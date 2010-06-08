package play.templates;

import groovy.lang.Closure;
import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

import play.Logger;
import play.i18n.Lang;
import play.i18n.Messages;
import play.libs.I18N;
import play.templates.Template.ExecutableTemplate.RawData;
import play.utils.HTML;

/**
 * Java extensions in templates
 */
public class JavaExtensions {

	/**
	 * Adds the String to the end of the array.
	 */
    public static String[] add(String[] array, String o) {
        String[] newArray = new String[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = o;
        return newArray;
    }

    /**
	 * Backslash-escapes Java-escaped single and double quotes in the object’s String representation.
	 */
    public static String addSlashes(Object o) {
        String string = o.toString();
        return string.replace("\"", "\\\"").replace("'", "\\'");
    }

    /**
	 * Formats the map’s keys and values as HTML attributes.
	 */
    public static RawData asAttr(Map attributes) {
        StringBuffer buf = new StringBuffer();
        for(Object key: attributes.keySet()) {
            buf.append(key+"=\""+attributes.get(key)+"\" ");
        }
        return new RawData(buf);
    }

    /**
	 * Formats the map’s keys and values as HTML attributes, if the condition is true.
	 */
    public static RawData asAttr(Map attributes, Object condition) {
        if(eval(condition)) {
            return asAttr(attributes);
        }
        return new RawData("");
    }

    /**
	 * Formats a time stamp as a date.
	 */
    public static String asdate(Long timestamp, String pattern) {
        return asdate(timestamp, pattern, Lang.get());
    }

    /**
	 * Formats a time stamp as a date, in the given language.
	 */
    public static String asdate(Long timestamp, String pattern, String lang) {
        return new SimpleDateFormat(pattern, new Locale(lang)).format(new Date(timestamp));
    }

    /**
	 * Parses the given XML string.
	 */
    public static GPathResult asXml(String xml) {
        try {
            return (new XmlSlurper()).parseText(xml);
        } catch (Exception e) {
            throw new RuntimeException("invalid XML");
        }
    }

    /**
	 * Formats the string in camel case, as if for a Java class name. 
	 */
    public static String camelCase(String string) {
        string = noAccents(string);
        string = string.replaceAll("[^\\w ]", "");
        StringBuilder result = new StringBuilder();
        for (String part : string.split(" ")) {
            result.append(capFirst(part));
        }
        return result.toString();
    }

    /**
	 * Capitalises every word in the object’s String representation.
	 */
    public static String capAll(Object o) {
        String string = o.toString();
        return capitalizeWords(string);
    }

    /**
	 * Capitalises the first word in the object’s String representation.
	 */
    public static String capFirst(Object o) {
        String string = o.toString();
        if (string.length() == 0) {
            return string;
        }
        return ("" + string.charAt(0)).toUpperCase() + string.substring(1);
    }

    /**
	 * Capitalises every word in the string.
	 */
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

    /**
	 * Returns true if the array contains the given string.
	 */
    public static boolean contains(String[] array, String value) {
        for (String v : array) {
            if (v.equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
	 * Removes occurrences of the given sub-string.
	 */
    public static String cut(Object o, String pattern) {
        String string = o.toString();
        return string.replace(pattern, "");
    }

    /**
	 * Returns true if the number is divisible by the given number – the divisor.
	 */
    public static boolean divisibleBy(Number n, int by) {
        return n.longValue() % by == 0;
    }

    /**
	 * Escapes reserved HTML characters in the object’s String representation.
	 */
    public static String escape(Object o) {
        String string = o.toString();
        return escapeHtml(string);
    }

    /**
	 * Escapes reserved HTML characters.
	 */
    public static String escapeHtml(String htmlToEscape) {
        return HTML.htmlEscape(htmlToEscape);
    }

    /**
	 * Escapes reserved JavaScript characters.
	 */
    public static String escapeJavaScript(String str) {
        return StringEscapeUtils.escapeJavaScript(str);
    }

    /**
	 * Escapes reserved XML characters.
	 */
    public static String escapeXml(String str) {
        return StringEscapeUtils.escapeXml(str);
    }

    protected static boolean eval(Object condition) {
        if(condition == null) return false;
        if(condition instanceof Boolean && !(Boolean)condition) return false;
        if(condition instanceof Collection && ((Collection)condition).size() == 0) return false;
        if(condition instanceof String && condition.toString().equals("")) return false;
        return true;
    }

    /**
	 * Formats the date using the given date format pattern.
	 */
    public static String format(Date date, String pattern) {
        return format(date, pattern, Lang.get());
    }
    
    /**
	 * Formats the date using the given date format pattern, in the given language.
	 */
    public static String format(Date date, String pattern, String lang) {
        return new SimpleDateFormat(pattern, new Locale(lang)).format(date);
    }

    /**
	 * Formats the number using the given number format pattern.
	 */
    public static String format(Number number, String pattern) {
        return new DecimalFormat(pattern).format(number);
    }

    /**
	 * Formats the number as a currency for the given currency code.
	 * 
	 * @param currencyCode An ISO 4217 currency code, e.g. EUR.
	 */
    public static String formatCurrency(Number number, String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(new Locale(Lang.get()));
        numberFormat.setCurrency(currency);
        numberFormat.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        String s = numberFormat.format(number);
        s = s.replace(currencyCode, I18N.getCurrencySymbol(currencyCode));
        return s;
    }

    /**
	 * Formats a number of bytes as a file size, with units.
	 */
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

    /**
     * Concatenate items of a collection as a string separated with <tt>separator</tt>
     *  items toString() method should be implemented to provide a string representation
     */
    public static String join(Collection items, String separator) {
        if (items == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        Iterator ite = items.iterator();
        int i = 0;
        while (ite.hasNext()) {
            if (i++ > 0) {
                sb.append(separator);
            }
            sb.append(ite.next());
        }
        return sb.toString();
    }

    /**
     * Return the last item of a list or null if the List is null
     */
    public static Object last(List<?> items) {
        return (items == null) ? null : items.get(items.size() - 1);
    }

    /**
	 * Replaces new-line characters with HTML br tags.
	 */
    public static RawData nl2br(Object data) {
        return new RawData(data.toString().replace("\n", "<br/>"));
    }

    /**
	 * Removes accents from the letters in the string.
	 */
    public static String noAccents(String string) {
        return string.replaceAll("[àáâãäåāąă]", "a").replaceAll("[çćčĉċ]", "c").replaceAll("[ďđð]", "d").replaceAll("[èéêëēęěĕė]", "e").replaceAll("[ƒſ]", "f").replaceAll("[ĝğġģ]", "g").replaceAll("[ĥħ]", "h").replaceAll("[ìíîïīĩĭįı]", "i").replaceAll("[ĳĵ]", "j").replaceAll("[ķĸ]", "k").replaceAll("[łľĺļŀ]", "l").replaceAll("[ñńňņŉŋ]", "n").replaceAll("[òóôõöøōőŏœ]", "o").replaceAll("[Þþ]", "p").replaceAll("[ŕřŗ]", "r").replaceAll("[śšşŝș]", "s").replaceAll("[ťţŧț]", "t").replaceAll("[ùúûüūůűŭũų]", "u").replaceAll("[ŵ]", "w").replaceAll("[ýÿŷ]", "y").replaceAll("[žżź]", "z").replaceAll("[æ]", "ae").replaceAll("[ÀÁÂÃÄÅĀĄĂ]", "A").replaceAll("[ÇĆČĈĊ]", "C").replaceAll("[ĎĐÐ]", "D").replaceAll("[ÈÉÊËĒĘĚĔĖ]", "E").replaceAll("[ĜĞĠĢ]", "G").replaceAll("[ĤĦ]", "H").replaceAll("[ÌÍÎÏĪĨĬĮİ]", "I").replaceAll("[Ĵ]", "J").replaceAll("[Ķ]", "K").replaceAll("[ŁĽĹĻĿ]", "L").replaceAll("[ÑŃŇŅŊ]", "N").replaceAll("[ÒÓÔÕÖØŌŐŎ]", "O").replaceAll("[ŔŘŖ]", "R").replaceAll("[ŚŠŞŜȘ]", "S").replaceAll("[ÙÚÛÜŪŮŰŬŨŲ]", "U").replaceAll("[Ŵ]", "W").replaceAll("[ÝŶŸ]", "Y").replaceAll("[ŹŽŻ]", "Z").replaceAll("[ß]", "ss");
    }

    /**
	 * Pads the string with &nbsp; up to the given length.
	 */
    public static String pad(String str, Integer size) {
        int t = size - str.length();
        for (int i = 0; i < t; i++) {
            str += "&nbsp;";
        }
        return str;
    }

    /**
	 * Returns the page number, for the given page size, from interpreting the number as an index.
	 */
    public static Integer page(Number number, Integer pageSize) {
        return number.intValue() / pageSize + (number.intValue() % pageSize > 0 ? 1 : 0);
    }

    /**
	 * Returns an ‘s’ when the collection’s size is not 1.
	 */
    public static String pluralize(Collection n) {
        return pluralize(n.size());
    }

    /**
	 * Returns the given plural when the collection’s size is not 1.
	 */
    public static String pluralize(Collection n, String plural) {
        return pluralize(n.size(), plural);
    }

    /**
	 * Returns the given plural form when the collection’s size is not 1; returns the given singular form when it is 1.
	 * 
	 * @param forms A two-element array containing the singular form, then the plural form.
	 */
    public static String pluralize(Collection n, String[] forms) {
        return pluralize(n.size(), forms);
    }

    /**
	 * Returns an ‘s’ when the number is not 1.
	 */
    public static String pluralize(Number n) {
        long l = n.longValue();
        if (l != 1) {
            return "s";
        }
        return "";
    }

    /**
	 * Returns the given plural when the number is not 1.
	 */
    public static String pluralize(Number n, String plural) {
        long l = n.longValue();
        if (l != 1) {
            return plural;
        }
        return "";
    }

    /**
	 * Returns the given plural form when the number is not 1; returns the given singular form when it is 1.
	 */
    public static String pluralize(Number n, String[] forms) {
        long l = n.longValue();
        if (l != 1) {
            return forms[1];
        }
        return forms[0];
    }

    /**
	 * Returns the object without template escaping.
	 */
    public static RawData raw(Object val) {
        return new RawData(val);
    }

    /**
	 * Returns the object without template escaping, if the condition is true.
	 */
    public static RawData raw(Object val, Object condition) {
        if(eval(condition)) {
            return new RawData(val);
        }
        return new RawData("");
    }

    /**
	 * Returns the array, with the given string removed.
	 */
    public static String[] remove(String[] array, String s) {
        List<String> temp = new ArrayList<String>(Arrays.asList(array));
        temp.remove(s);
        return (String[]) temp.toArray(new String[temp.size()]);
    }

    /**
	 * Formats the date as a relative time, compared to now, e.g. 3 minutes ago.
	 */
    public static String since(Date date) {
        return since(date, false);
    }

    /**
	 * Formats the date as a relative time, compared to now, e.g. 3 minutes ago.
	 * 
	 * @param stopAtMonth If true, then dates more than a month ago are formatted as a date string, e.g. Jan 1, 2010.
	 */
    public static String since(Date date, Boolean stopAtMonth) {
        Date now = new Date();
        if (now.before(date)) {
            return "";
        }
        long delta = (now.getTime() - date.getTime()) / 1000;
        if (delta < 60) {
            return Messages.get("since.seconds", delta, pluralize(delta));
        }
        if (delta < 60 * 60) {
            long minutes = delta / 60;
            return Messages.get("since.minutes", minutes, pluralize(minutes));
        }
        if (delta < 24 * 60 * 60) {
            long hours = delta / (60 * 60);
            return Messages.get("since.hours", hours, pluralize(hours));
        }
        if (delta < 30 * 24 * 60 * 60) {
            long days = delta / (24 * 60 * 60);
            return Messages.get("since.days", days, pluralize(days));
        }
        if (stopAtMonth)
        {
            return asdate(date.getTime(), Messages.get("since.format"));
        }
        if (delta < 365 * 24 * 60 * 60) {
            long months = delta / (30 * 24 * 60 * 60);
            return Messages.get("since.months", months, pluralize(months));
        }
        long years = delta / (365 * 24 * 60 * 60);
        return Messages.get("since.years", years, pluralize(years));
    }

    /**
	 * Formats the string as a ‘slug’ for use in URLs, that avoids reserved URL path characters
	 */
    public static String slugify(String string) {
        string = noAccents(string);
        return string.replaceAll("[^\\w]", "-").replaceAll("-{2,}", "-").replaceAll("-$", "").toLowerCase();
    }

    public static String toString(Closure closure) {
        PrintWriter oldWriter = (PrintWriter) closure.getProperty("out");
        StringWriter newWriter = new StringWriter();
        closure.setProperty("out", new PrintWriter(newWriter));
        closure.call();
        closure.setProperty("out", oldWriter);
        return newWriter.toString();
    }

    /**
	 * Escapes reserved URL query string characters.
	 */
    public static String urlEncode(String entity) {
        try {
            return URLEncoder.encode(entity, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Logger.error(e, entity);
        }
        return entity;
    }

    /**
	 * Returns the first parameter (‘yes’) if the object evaluates to true, or the second parameter (‘no’) otherwise.
	 */
    public static String yesno(Object o, String[] values) {
        boolean value = play.templates.FastTags._evaluateCondition(o);
        if (value) {
            return values[0];
        }
        return values[1];
    }
}
