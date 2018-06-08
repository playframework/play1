package play.libs;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeTest {
    @Test
    public void parseWithNullArgument() {
        int duration = Time.parseDuration(null);
        assertEquals(duration, 2592000);
    }

    @Test
    public void parseGood1() {
        int duration = Time.parseDuration("40s");
        assertEquals(duration, 40);
    }

    @Test
    public void parseGood2() {
        int duration = Time.parseDuration("2d4h10s");
        assertEquals(duration, 187210);
    }

    @Test
    public void parseGood3() {
        int duration = Time.parseDuration("0d4h10s");
        assertEquals(duration, 14410);
    }

    @Test
    public void parseGood4() {
        int duration = Time.parseDuration("2h");
        assertEquals(duration, 7200);
    }

    @Test
    public void parseGood5() {
        int duration = Time.parseDuration("120min");
        assertEquals(duration, 7200);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseBad1() {
        int duration = Time.parseDuration("1w2d3h10s");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseBad2() {
        int duration = Time.parseDuration("foobar");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseBad3() {
        int duration = Time.parseDuration("20xyz");
    }
}
