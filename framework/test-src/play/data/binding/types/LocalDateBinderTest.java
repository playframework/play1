package play.data.binding.types;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.PlayBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;


public class LocalDateBinderTest {

    private LocalDateBinder binder = new LocalDateBinder();

    @BeforeEach
    public void setup() {
        new PlayBuilder().build();
    }

    @Test
    public void nullLocalDate() {
        assertNull(binder.bind("event.start", null, null, LocalDate.class, null));
    }

    @Test
    public void emptyLocalDate() {
        assertNull(binder.bind("event.start", null, "", LocalDate.class, null));
    }

    @Test
    public void validLocalDate() {
        LocalDate expected = LocalDate.parse("2007-12-03");
        LocalDate actual = binder.bind("event.start", null, "2007-12-03", LocalDate.class, null);
        assertEquals(expected, actual);
    }

    @Test
    public void invalidLocalDate() {
        assertThrows(DateTimeParseException.class, () -> {
            binder.bind("event.start", null, "2007-13-03", LocalDate.class, null);
        });
    }
}