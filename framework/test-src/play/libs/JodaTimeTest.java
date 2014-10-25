package play.libs;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Assert;
import org.junit.Test;

import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;

/**
 * Russia will subtract an hour from most of its time zones on 2014-10-26 at 02:00 local time.
 * Updated timezone file has version 2014f, but Joda-time 2.3 in current Play Framework
 * distribution has 2014e version only. So it's time to check and update.
 */
public class JodaTimeTest extends Assert {

    @Test
    public void test() {
        DateTimeZone zone = DateTimeZone.forTimeZone(TimeZone.getTimeZone( "Europe/Moscow"));
        DateTimeFormatter df = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss z");

        DateTime dateBefore = new DateTime(1413769091L * 1000L, zone);
        assertThat(df.print(dateBefore), is("2014-10-20 05:38:11 MSK"));

        DateTime dateAfter = new DateTime( 1414633091L * 1000L, zone);
        assertThat(df.print(dateAfter), is("2014-10-30 04:38:11 MSK"));
    }
}
