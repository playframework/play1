package play.libs;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    
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
    
    public static class Time {
        
        static Pattern hours = Pattern.compile("^([0-9]+)h$");
        static Pattern minutes = Pattern.compile("^([0-9]+)mn$");
        static Pattern seconds = Pattern.compile("^([0-9]+)s$");

        public static Long parseDuration(String duration) {
            if (duration == null) {
                return Long.MAX_VALUE;
            }
            Long toAdd = null;
            if (hours.matcher(duration).matches()) {
                Matcher matcher = hours.matcher(duration);
                matcher.matches();
                toAdd = Long.parseLong(matcher.group(1)) * (60 * 60 * 1000);
            } else if (minutes.matcher(duration).matches()) {
                Matcher matcher = minutes.matcher(duration);
                matcher.matches();
                toAdd = Long.parseLong(matcher.group(1)) * (60 * 1000);
            } else if (seconds.matcher(duration).matches()) {
                Matcher matcher = seconds.matcher(duration);
                matcher.matches();
                toAdd = Long.parseLong(matcher.group(1)) * (1000);
            }
            if (toAdd == null) {
                throw new IllegalArgumentException("Invalid duration pattern : " + duration);
            }
            return System.currentTimeMillis() + toAdd;
        }
        
    }
    
}
