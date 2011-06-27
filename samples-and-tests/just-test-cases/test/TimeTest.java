import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.Test;

import play.Logger;
import play.libs.Time;
import play.test.UnitTest;


public class TimeTest extends UnitTest {

    static final String CRON_DAILY = "0 0 12 * * ?";
    static final long MILLI_IN_A_DAY = 24 * 3600 * 1000;

    static final String CRON_WEEKLY = "0 0 12 ? * MON";
    static final long MILLI_IN_A_WEEK = MILLI_IN_A_DAY * 7;

    @Test
    public void cron() {
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(2011, 01, 01);
        assertEquals(MILLI_IN_A_DAY, Time.cronInterval(CRON_DAILY, cal.getTime()));
        assertEquals(MILLI_IN_A_WEEK, Time.cronInterval(CRON_WEEKLY, cal.getTime()));
    }

}
