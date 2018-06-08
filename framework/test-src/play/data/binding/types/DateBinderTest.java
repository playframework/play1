package play.data.binding.types;

import org.junit.Before;
import org.junit.Test;
import play.Play;
import play.PlayBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DateBinderTest {
    private DateBinder binder = new DateBinder();

    @Before
    public void setup() {
        new PlayBuilder().build();
    }

    @Test
    public void parses_date_in_play_format() throws Exception {
        Play.configuration.setProperty("date.format", "dd.MM.yyyy");

        Date actual = binder.bind("client.birthday", null, "31.12.1986", Date.class, null);
        Date expected = new SimpleDateFormat("MM/dd/yyyy").parse("12/31/1986");
        assertEquals(expected, actual);
    }

    @Test
    public void parses_date_in_iso_format() throws Exception {
        Date actual = binder.bind("client.birthday", null, "ISO8601:1986-04-12T00:00:00+0500", Date.class, null);
        Date expected = new SimpleDateFormat("MM/dd/yyyyZ").parse("04/12/1986+0500");
        assertEquals(expected, actual);
    }

    @Test
    public void parses_null_to_null() throws Exception {
        assertNull(binder.bind("client.birthday", null, null, Date.class, null));
    }

    @Test
    public void parses_empty_string_to_null() throws Exception {
        assertNull(binder.bind("client.birthday", null, "", Date.class, null));
    }

    @Test(expected = ParseException.class)
    public void throws_ParseException_for_invalid_value() throws Exception {
        binder.bind("client.birthday", null, "12/31/1986", Date.class, null);
    }
}