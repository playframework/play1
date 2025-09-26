package play.data.binding.types;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.PlayBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

public class LocalDateTimeBinderTest {

    private LocalDateTimeBinder binder = new LocalDateTimeBinder();

    @BeforeEach
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

    @Test
    public void invalidLocalDateTime() {
        assertThrows(DateTimeParseException.class, () -> {
            binder.bind("event.start", null, "2007-13-03T10:15:30", LocalDateTime.class, null);
        });
    }
}