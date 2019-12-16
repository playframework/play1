package play.data.binding.types;

import org.junit.Before;
import org.junit.Test;
import play.PlayBuilder;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LocalTimeBinderTest {

    private LocalTimeBinder binder = new LocalTimeBinder();

    @Before
    public void setup() {
        new PlayBuilder().build();
    }

    @Test
    public void nullLocalTime() {
        assertNull(binder.bind("event.start", null, null, LocalTime.class, null));
    }

    @Test
    public void emptyLocalTime() {
        assertNull(binder.bind("event.start", null, "", LocalTime.class, null));
    }

    @Test
    public void validLocalTime() {
        LocalTime expected = LocalTime.parse("10:15:30");
        LocalTime actual = binder.bind("event.start", null, "10:15:30", LocalTime.class, null);
        assertEquals(expected, actual);
    }

    @Test(expected = DateTimeParseException.class)
    public void invalidLocalTime() throws Exception {
        binder.bind("event.start", null, "25:15:30", LocalTime.class, null);
    }
}