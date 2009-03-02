package play.libs;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Generic utils
 */
public class Utils {
    
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
    }
    
    
    private static ThreadLocal<SimpleDateFormat> httpFormatter = new ThreadLocal<SimpleDateFormat>();

    public static SimpleDateFormat getHttpDateFormatter() {
        if (httpFormatter.get() == null) {
            httpFormatter.set(new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US));
            httpFormatter.get().setTimeZone(TimeZone.getTimeZone("GMT"));
        }
        return httpFormatter.get();
    }
}
