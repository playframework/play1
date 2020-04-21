package play.data.binding.types;

import org.junit.Before;
import org.junit.Test;
import play.PlayBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LocalDateTimeBinderTest {

    private LocalDateTimeBinder binder = new LocalDateTimeBinder();

    @Before
    public void setup() {
        new PlayBuilder().build();
    }

    @Test
    public void nullLocalDateTime() {
        assertNull(binder.bind("event.start", null, null, LocalDateTime.class, null));
    }

    @Test
    public void emptyLocalDateTime() {
        assertNull(binder.bind("event.start", null, "", LocalDateTime.class, null));
    }

    @Test
    public void validLocalDateTime() {
        LocalDateTime expected = LocalDateTime.parse("2007-12-03T10:15:30");
        LocalDateTime actual = binder.bind("event.start", null, "2007-12-03T10:15:30", LocalDateTime.class, null);
        assertEquals(expected, actual);
    }

    @Test(expected = DateTimeParseException.class)
    public void invalidLocalDateTime() throws Exception {
        binder.bind("event.start", null, "2007-13-03T10:15:30", LocalDateTime.class, null);
    }
}