package play.libs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class TimeTest {
    @Test
    public void parseWithNullArgument() {
        int duration = Time.parseDuration(null);
        assertEquals(2592000, duration);
    }

    @Test
    public void parseGood1() {
        int duration = Time.parseDuration("40s");
        assertEquals(40 , duration);
    }

    @Test
    public void parseGood2() {
        int duration = Time.parseDuration("2d4h10s");
        assertEquals(187210, duration);
    }

    @Test
    public void parseGood3() {
        int duration = Time.parseDuration("0d4h10s");
        assertEquals(14410, duration);
    }

    @Test
    public void parseGood4() {
        int duration = Time.parseDuration("2h");
        assertEquals(7200, duration);
    }

    @Test
    public void parseGood5() {
        int duration = Time.parseDuration("120min");
        assertEquals(7200, duration);
    }

    @Test
    public void parseBad1() {
        assertThrows(IllegalArgumentException.class, () -> {
            Time.parseDuration("1w2d3h10s");
        });
    }

    @Test
    public void parseBad2() {
        assertThrows(IllegalArgumentException.class, () -> {
            Time.parseDuration("foobar");
        });
    }

    @Test
    public void parseBad3() {
        assertThrows(IllegalArgumentException.class, () -> {
            Time.parseDuration("20xyz");
        });
    }
}
