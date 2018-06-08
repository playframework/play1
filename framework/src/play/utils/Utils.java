package play.utils;

import java.lang.annotation.Annotation;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import play.Play;
import play.mvc.Scope;
import play.vfs.VirtualFile;

/**
 * Generic utils
 */
public class Utils {

    public static <T> String join(Iterable<T> values, String separator) {
        if (values == null) {
            return "";
        }
        Iterator<T> iter = values.iterator();
        if (!iter.hasNext()) {
            return "";
        }
        StringBuilder toReturn = new StringBuilder(String.valueOf(iter.next()));
        while (iter.hasNext()) {
            toReturn.append(separator).append(iter.next());
        }
        return toReturn.toString();
    }

    public static String join(String[] values, String separator) {
        return (values == null) ? "" : join(Arrays.asList(values), separator);
    }

    public static String join(Annotation[] values, String separator) {
        return (values == null) ? "" : join(Arrays.asList(values), separator);
    }

    public static String getSimpleNames(Annotation[] values) {
        if (values == null) {
            return "";
        }
        List<Annotation> a = Arrays.asList(values);
        Iterator<Annotation> iter = a.iterator();
        if (!iter.hasNext()) {
            return "";
        }
        StringBuilder toReturn = new StringBuilder("@" + iter.next().annotationType().getSimpleName());
        while (iter.hasNext()) {
            toReturn.append(", @").append(iter.next().annotationType().getSimpleName());
        }
        return toReturn.toString();
    }

    /**
     * Get the list of annotations in string
     * 
     * @param values
     *            Annotations to format
     * @return The string representation of the annotations
     * @deprecated Use Utils.join(values, " ");
     */
    @Deprecated
    public static String toString(Annotation[] values) {
        return join(values, " ");
    }

    public static String open(String file, Integer line) {
        if (Play.configuration.containsKey("play.editor")) {
            VirtualFile vfile = VirtualFile.fromRelativePath(file);
            if (vfile != null) {
                return String.format(Play.configuration.getProperty("play.editor"), vfile.getRealFile().getAbsolutePath(), line);
            }
        }
        return null;
    }

    /**
     * for java.util.Map
     */
    public static class Maps {

        public static void mergeValueInMap(Map<String, String[]> map, String name, String value) {
            String[] newValues = null;
            String[] oldValues = map.get(name);
            if (oldValues == null) {
                newValues = new String[1];
                newValues[0] = value;
            } else {
                newValues = new String[oldValues.length + 1];
                System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
                newValues[oldValues.length] = value;
            }
            map.put(name, newValues);
        }

        public static void mergeValueInMap(Map<String, String[]> map, String name, String[] values) {
            for (String value : values) {
                mergeValueInMap(map, name, value);
            }
        }

        public static <K, V> Map<K, V> filterMap(Map<K, V> map, String keypattern) {
            try {
                @SuppressWarnings("unchecked")
                Map<K, V> filtered = map.getClass().newInstance();
                for (Map.Entry<K, V> entry : map.entrySet()) {
                    K key = entry.getKey();
                    if (key.toString().matches(keypattern)) {
                        filtered.put(key, entry.getValue());
                    }
                }
                return filtered;
            } catch (Exception iex) {
                return null;
            }
        }
    }

    private static final ThreadLocal<SimpleDateFormat> httpFormatter = new ThreadLocal<>();

    public static SimpleDateFormat getHttpDateFormatter() {
        if (httpFormatter.get() == null) {
            httpFormatter.set(new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US));
            httpFormatter.get().setTimeZone(TimeZone.getTimeZone("GMT"));
        }
        return httpFormatter.get();
    }

    public static Map<String, String[]> filterMap(Map<String, String[]> map, String prefix) {
        Map<String, String[]> newMap = new HashMap<>();
        for (String key : map.keySet()) {
            if (!key.startsWith(prefix + ".")) {
                newMap.put(key, map.get(key));
            }
        }
        return newMap;
    }

    public static Map<String, String> filterParams(Scope.Params params, String prefix) {
        return filterParams(params.all(), prefix);
    }

    public static Map<String, String> filterParams(Map<String, String[]> params, String prefix, String separator) {
        Map<String, String> filteredMap = new LinkedHashMap<>();
        prefix += ".";
        for (Map.Entry<String, String[]> e : params.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                filteredMap.put(e.getKey().substring(prefix.length()), Utils.join(e.getValue(), separator));
            }
        }
        return filteredMap;
    }

    public static Map<String, String> filterParams(Map<String, String[]> params, String prefix) {
        return filterParams(params, prefix, ", ");
    }

    public static void kill(String pid) throws Exception {
        String command = OS.isWindows() ? "taskkill /F /PID " + pid : "kill " + pid;
        Runtime.getRuntime().exec(command).waitFor();
    }

    public static class AlternativeDateFormat {

        Locale locale;
        List<SimpleDateFormat> formats = new ArrayList<>();

        public AlternativeDateFormat(Locale locale, String... alternativeFormats) {
            super();
            this.locale = locale;
            setFormats(alternativeFormats);
        }

        public void setFormats(String... alternativeFormats) {
            for (String format : alternativeFormats) {
                formats.add(new SimpleDateFormat(format, locale));
            }
        }

        public Date parse(String source) throws ParseException {
            for (SimpleDateFormat dateFormat : formats) {
                if (source.length() == dateFormat.toPattern().replace("\'", "").length()) {
                    try {
                        return dateFormat.parse(source);
                    } catch (ParseException ex) {
                    }
                }
            }
            throw new ParseException("Date format not understood", 0);
        }

        static final ThreadLocal<AlternativeDateFormat> dateformat = new ThreadLocal<>();

        public static AlternativeDateFormat getDefaultFormatter() {
            if (dateformat.get() == null) {
                dateformat.set(new AlternativeDateFormat(Locale.US, "yyyy-MM-dd'T'HH:mm:ss'Z'", // ISO8601 + timezone
                        "yyyy-MM-dd'T'HH:mm:ss", // ISO8601
                        "yyyy-MM-dd HH:mm:ss", "yyyyMMdd HHmmss", "yyyy-MM-dd", "yyyyMMdd'T'HHmmss", "yyyyMMddHHmmss", "dd'/'MM'/'yyyy",
                        "dd-MM-yyyy", "dd'/'MM'/'yyyy HH:mm:ss", "dd-MM-yyyy HH:mm:ss", "ddMMyyyy HHmmss", "ddMMyyyy"));
            }
            return dateformat.get();
        }
    }

    public static String urlDecodePath(String enc) {
        try {
            return URLDecoder.decode(enc.replaceAll("\\+", "%2B"), Play.defaultWebEncoding);
        } catch (Exception e) {
            return enc;
        }
    }

    public static String urlEncodePath(String plain) {
        try {
            return URLEncoder.encode(plain, Play.defaultWebEncoding);
        } catch (Exception e) {
            return plain;
        }
    }
}
