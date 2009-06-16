package play.libs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Time utils
 */
public class Time {

    static Pattern days = Pattern.compile("^([0-9]+)d$");
    static Pattern hours = Pattern.compile("^([0-9]+)h$");
    static Pattern minutes = Pattern.compile("^([0-9]+)mn$");
    static Pattern seconds = Pattern.compile("^([0-9]+)s$");

    /**
     * Parse a duration
     * @param duration 3h, 2mn, 7s
     * @return The number of seconds
     */
    public static int parseDuration(String duration) {
        if (duration == null) {
            return 60 * 60 * 24 * 365;
        }
        int toAdd = -1;
        if (days.matcher(duration).matches()) {
            Matcher matcher = days.matcher(duration);
            matcher.matches();
            toAdd = Integer.parseInt(matcher.group(1)) * (60 * 60) * 24;
        } else if (hours.matcher(duration).matches()) {
            Matcher matcher = hours.matcher(duration);
            matcher.matches();
            toAdd = Integer.parseInt(matcher.group(1)) * (60 * 60);
        } else if (minutes.matcher(duration).matches()) {
            Matcher matcher = minutes.matcher(duration);
            matcher.matches();
            toAdd = Integer.parseInt(matcher.group(1)) * (60);
        } else if (seconds.matcher(duration).matches()) {
            Matcher matcher = seconds.matcher(duration);
            matcher.matches();
            toAdd = Integer.parseInt(matcher.group(1));
        }
        if (toAdd == -1) {
            throw new IllegalArgumentException("Invalid duration pattern : " + duration);
        }
        return toAdd;
    }
}
