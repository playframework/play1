package play.data.binding.types;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.Play;
import play.PlayBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class CalendarBinderTest {

    private CalendarBinder binder = new CalendarBinder();

    @BeforeEach
    public void setup() {
        new PlayBuilder().build();
    }
    
    @Test
    public void parses_date_to_calendar() throws Exception {
        Play.configuration.setProperty("date.format", "dd.MM.yyyy");
        Date expected = new SimpleDateFormat("dd.MM.yyyy").parse("31.12.1986");
        Calendar actual = binder.bind("client.birthday", null, "31.12.1986", Calendar.class, null);
        assertEquals(expected, actual.getTime());
    }
    
    @Test
    public void parses_null_to_null() throws Exception {
        assertNull(binder.bind("client.birthday", null, null, Calendar.class, null));
    }
    
    @Test
    public void parses_empty_string_to_null() throws Exception {
        assertNull(binder.bind("client.birthday", null, "", Calendar.class, null));
    }

    @Test
    public void throws_ParseException_for_invalid_value() {
        assertThrows(ParseException.class, () -> {
            binder.bind("client.birthday", null, "12/31/1986", Calendar.class, null);
        });
    }
}