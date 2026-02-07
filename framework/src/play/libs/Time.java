package play.libs;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Time utils
 *
 * Provides a parser for time expression.
 * <p>
 * Time expressions provide the ability to specify complex time combinations
 * such as &quot;2d&quot;, &quot;1w2d3h10s&quot; or &quot;2d4h10s&quot;.
 * </p>
 *
 */
public class Time {
    private static final Pattern p = Pattern.compile("(([0-9]+?)((d|h|mi|min|mn|s)))+?");
    private static final int MINUTE = 60;
    private static final int HOUR = 60 * MINUTE;
    private static final int DAY = 24 * HOUR;

    /**
     * Parse a duration
     *
     * @param duration
     *            3h, 2mn, 7s or combination 2d4h10s, 1w2d3h10s
     * @return The number of seconds
     */
    public static int parseDuration(String duration) {
        if (duration == null) {
            return 30 * DAY;
        }

        Matcher matcher = p.matcher(duration);
        int seconds = 0;
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid duration pattern : " + duration);
        }

        matcher.reset();
        while (matcher.find()) {
            switch (matcher.group(3)) {
                case "d" -> seconds += Integer.parseInt(matcher.group(2)) * DAY;
                case "h" -> seconds += Integer.parseInt(matcher.group(2)) * HOUR;
                case "mi", "min", "mn" -> seconds += Integer.parseInt(matcher.group(2)) * MINUTE;
                default -> seconds += Integer.parseInt(matcher.group(2));
            }
        }

        return seconds;
    }

    /**
     * Parse a CRON expression
     *
     * @param cron
     *            The CRON String
     * @return The next Date that satisfy the expression
     */
    public static Date parseCRONExpression(String cron) {
        try {
            return new CronExpression(cron).getNextValidTimeAfter(new Date());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid CRON pattern : " + cron, e);
        }
    }

    /**
     * Compute the number of milliseconds between the next valid date and the
     * one after
     *
     * @param cron
     *            The CRON String
     * @return the number of milliseconds between the next valid date and the
     *         one after, with an invalid interval between
     */
    public static long cronInterval(String cron) {
        return cronInterval(cron, new Date());
    }

    /**
     * Compute the number of milliseconds between the next valid date and the
     * one after
     *
     * @param cron
     *            The CRON String
     * @param date
     *            The date to start search
     * @return the number of milliseconds between the next valid date and the
     *         one after, with an invalid interval between
     */
    public static long cronInterval(String cron, Date date) {
        try {
            return new CronExpression(cron).getNextInterval(date);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid CRON pattern : " + cron, e);
        }
    }
}
