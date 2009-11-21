package play.utils;

import java.lang.annotation.Annotation;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import play.Play;

/**
 * Generic utils
 */
public class Utils {

    public static String toString(Annotation[] annotations) {
        String toReturn = "";
        if (annotations != null) {
            for (Annotation annotation : annotations) {
                toReturn += annotation.toString() + " ";
            }
        }
        return toReturn;
    }

    public static String open(String file, Integer line) {
        if (Play.configuration.containsKey("play.editor")) {
            return String.format(Play.configuration.getProperty("play.editor"), Play.getFile(file).getAbsolutePath(), line);
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

        public static Map filterMap(Map<?,?> map, String keypattern) {
            try {
                Map filtered = (Map) map.getClass().newInstance();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                	Object key = entry.getKey();
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
    private static ThreadLocal<SimpleDateFormat> httpFormatter = new ThreadLocal<SimpleDateFormat>();

    public static SimpleDateFormat getHttpDateFormatter() {
        if (httpFormatter.get() == null) {
            httpFormatter.set(new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US));
            httpFormatter.get().setTimeZone(TimeZone.getTimeZone("GMT"));
        }
        return httpFormatter.get();
    }

    public static class AlternativeDateFormat {

        Locale locale;
        List<SimpleDateFormat> formats = new ArrayList<SimpleDateFormat>();

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
        static ThreadLocal<AlternativeDateFormat> dateformat = new ThreadLocal<AlternativeDateFormat>();

        public static AlternativeDateFormat getDefaultFormatter() {
            if (dateformat.get() == null) {
                dateformat.set(new AlternativeDateFormat(Locale.US,
                        "yyyy-MM-dd'T'hh:mm:ss'Z'", // ISO8601 + timezone
                        "yyyy-MM-dd'T'hh:mm:ss", // ISO8601
                        "yyyy-MM-dd hh:mm:ss",
                        "yyyyMMdd hhmmss",
                        "yyyy-MM-dd",
                        "yyyyMMdd'T'hhmmss",
                        "yyyyMMddhhmmss",
                        "dd'/'MM'/'yyyy",
                        "dd-MM-yyyy",
                        "dd'/'MM'/'yyyy hh:mm:ss",
                        "dd-MM-yyyy hh:mm:ss",
                        "ddMMyyyy hhmmss",
                        "ddMMyyyy"));
            }
            return dateformat.get();
        }
    }
}
