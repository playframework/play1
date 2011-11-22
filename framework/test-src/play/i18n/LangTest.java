/*
 * (C) 2011 IMIS group AG, Zurich, Switzerland.
 */
package play.i18n;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Locale;

import org.junit.Test;

public class LangTest {
    
    @Test
    public void testGetLocaleLanguage() {
        Locale locale = Lang.getLocale("de");
        assertThat(locale.getCountry(), is(""));
        assertThat(locale.getLanguage(), is("de"));
    }
    
    @Test
    public void testGetLocaleCountry() {
        Locale locale = Lang.getLocale("en_US");
        assertThat(locale.getCountry(), is("US"));
        assertThat(locale.getLanguage(), is("en"));
    }
}
