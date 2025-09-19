package play.data.binding.types;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.PlayBuilder;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LocalTimeBinderTest {

    private LocalTimeBinder binder = new LocalTimeBinder();

    @BeforeEach
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

    @Test
    public void invalidLocalTime() throws Exception {
        assertThrows(DateTimeParseException.class, () -> {
            binder.bind("event.start", null, "25:15:30", LocalTime.class, null);
        });
    }
}