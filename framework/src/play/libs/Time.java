package play.libs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Time {
        
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
