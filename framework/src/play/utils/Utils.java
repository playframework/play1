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
import java.util.StringJoiner;
import java.util.TimeZone;
import java.util.function.BiFunction;

import play.Play;
import play.mvc.Scope;
import play.vfs.VirtualFile;

import static java.util.Objects.requireNonNull;

/**
 * Generic utils
 */
public class Utils {

    public static <T> String join(Iterable<T> values, String separator) {
        if (values == null) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(separator);
        for (T value : values) {
            joiner.add(String.valueOf(value));
        }

        return joiner.toString();
    }

    public static String join(String[] values, String separator) {
        return (values == null) ? "" : String.join(separator, values);
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

        private static final BiFunction<String[], String[], String[]> MERGE_MAP_VALUES = (oldValues, newValues) -> {
            String[] merged = new String[oldValues.length + newValues.length];
            System.arraycopy(oldValues, 0, merged, 0, oldValues.length);
            System.arraycopy(newValues, 0, merged, oldValues.length, newValues.length);

            return merged;
        };

        public static void mergeValueInMap(Map<String, String[]> map, String name, String value) {
            map.merge(name, new String[]{ value }, MERGE_MAP_VALUES);
        }

        public static void mergeValueInMap(Map<String, String[]> map, String name, String[] values) {
            map.merge(name, requireNonNull(values), MERGE_MAP_VALUES);
        }

        public static <K, V> Map<K, V> filterMap(Map<K, V> map, String keypattern) {
            try {
                @SuppressWarnings("unchecked")
                Map<K, V> filtered = map.getClass().getDeclaredConstructor().newInstance();
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

    private static final ThreadLocal<SimpleDateFormat> httpFormatter = ThreadLocal.withInitial(() -> {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));

        return format;
    });

    public static SimpleDateFormat getHttpDateFormatter() {
        return httpFormatter.get();
    }

    public static Map<String, String[]> filterMap(Map<String, String[]> map, String prefix) {
        prefix += '.';
        Map<String, String[]> newMap = new HashMap<>(map.size());
        for (String key : map.keySet()) {
            if (!key.startsWith(prefix)) {
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
        prefix += '.';
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
        String[] cmdarray = { OS.isWindows() ? "taskkill /F /PID " + pid : "kill " + pid };
        Runtime.getRuntime().exec(cmdarray).waitFor();
    }

    public static class AlternativeDateFormat {

        final List<SimpleDateFormat> formats = new ArrayList<>();
        final Locale locale;

        public AlternativeDateFormat(Locale locale, String... alternativeFormats) {
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

        static final ThreadLocal<AlternativeDateFormat> dateformat = ThreadLocal.withInitial(() ->
            new AlternativeDateFormat(
                Locale.US,
                "yyyy-MM-dd'T'HH:mm:ss'Z'", // ISO8601 + timezone
                "yyyy-MM-dd'T'HH:mm:ss", // ISO8601
                "yyyy-MM-dd HH:mm:ss",
                "yyyyMMdd HHmmss",
                "yyyy-MM-dd",
                "yyyyMMdd'T'HHmmss",
                "yyyyMMddHHmmss",
                "dd'/'MM'/'yyyy",
                "dd-MM-yyyy",
                "dd'/'MM'/'yyyy HH:mm:ss",
                "dd-MM-yyyy HH:mm:ss",
                "ddMMyyyy HHmmss",
                "ddMMyyyy"
            )
        );

        public static AlternativeDateFormat getDefaultFormatter() {
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
