package play.i18n;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import play.Play;
import play.PlayBuilder;
import play.mvc.Http;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.test.FunctionalTest;

public class LangTest {

	@Test
	public void testFirefoxExample() {
		Play.langs = Arrays.asList("zh", "en");
		Play.configuration = new Properties();
		
		Header header = new Header("accept-language", "zh_cn,en;q=0.3,en_us;q=0.7");
		Map<String, Header> headers = new HashMap<>();
		headers.put(header.name, header);
		
		Request request = Request.createRequest(null, null, null, null, null, null, null, null, false, 0, null, false, headers , null);
		Http.Request.current.set(request);
		
		String language = Lang.get();
		Assert.assertEquals("zh", language);
	}
	
    @Test
    public void testChange() {
        new PlayBuilder().build();
        Play.langs = Arrays.asList("no", "en", "fr");
        Lang.current.set(null);
        assertThat(Lang.current.get()).isNull();

        Lang.change("no");
        assertThat(Lang.current.get()).isEqualTo("no");
        Lang.change("nox");
        assertThat(Lang.current.get()).isEqualTo("no");
        Lang.change("EN");
        assertThat(Lang.current.get()).isEqualTo("en");
        Lang.change("fr");
        assertThat(Lang.current.get()).isEqualTo("fr");

        Lang.change("xx");
        assertThat(Lang.current.get()).isEqualTo("fr");

        Lang.change("en_uk");
        assertThat(Lang.current.get()).isEqualTo("en");

        Play.langs = Arrays.asList("no", "en", "en_uk", "fr");
        Lang.current.set(null);
        Lang.change("en_uk");
        assertThat(Lang.current.get()).isEqualTo("en_uk");
        Lang.change("en");
        assertThat(Lang.current.get()).isEqualTo("en");
        Lang.change("en_qw");
        assertThat(Lang.current.get()).isEqualTo("en");
    }

    @Test
    public void testGet() {
        new PlayBuilder().build();
        Play.langs = Arrays.asList("no", "en", "en_GB", "fr");
        Lang.current.set(null);

        Http.Response.current.set( new Http.Response());

        // check default when missing request
        Http.Request.current.set(null);
        assertThat(Lang.get()).isEqualTo("no");

        // check default when missing info in request
        Http.Request req = FunctionalTest.newRequest();
        Http.Request.current.set(req);
        Lang.current.set(null);
        assertLocale(new Locale("no"));

        // check only with accept-language,  without cookie value
        req = FunctionalTest.newRequest();
        req.headers.put("accept-language", new Http.Header("accept-language", "x"));
        Http.Request.current.set(req);
        Lang.current.set(null);
        assertLocale(new Locale("no"));

        req = FunctionalTest.newRequest();
        req.headers.put("accept-language", new Http.Header("accept-language", "no"));
        Http.Request.current.set(req);
        Lang.current.set(null);
        assertLocale(new Locale("no"));

        req = FunctionalTest.newRequest();
        req.headers.put("accept-language", new Http.Header("accept-language", "en"));
        Http.Request.current.set(req);
        Lang.current.set(null);
        assertLocale(new Locale("en"));

        req = FunctionalTest.newRequest();
        req.headers.put("accept-language", new Http.Header("accept-language", "x,en"));
        Http.Request.current.set(req);
        Lang.current.set(null);
        assertLocale(new Locale("en"));

        req = FunctionalTest.newRequest();
        req.headers.put("accept-language", new Http.Header("accept-language", "en-GB"));
        Http.Request.current.set(req);
        Lang.current.set(null);
        assertLocale(new Locale("en", "GB"));

        req = FunctionalTest.newRequest();
        req.headers.put("accept-language", new Http.Header("accept-language", "x,en-GB"));
        Http.Request.current.set(req);
        Lang.current.set(null);
        assertLocale(new Locale("en", "GB"));

        req = FunctionalTest.newRequest();
        req.headers.put("accept-language", new Http.Header("accept-language", "x,en-US"));
        Http.Request.current.set(req);
        Lang.current.set(null);
        assertLocale(new Locale("en"));

	
	// prove lighthouse fix https://play.lighthouseapp.com/projects/57987/tickets/1302
	// space in accept language header
        req = FunctionalTest.newRequest();
        req.headers.put("accept-language", new Http.Header("accept-language", "nl, en;q=0.8"));
        Http.Request.current.set(req);
        Lang.current.set(null);
        assertLocale(new Locale("en"));
        // check with cookie value

        req = FunctionalTest.newRequest();

        Http.Cookie cookie = new Http.Cookie();
        cookie.name = "PLAY_LANG";
        cookie.value = "x";//not found in cookie
        req.cookies.put(cookie.name, cookie);
        req.headers.put("accept-language", new Http.Header("accept-language", "en"));
        Http.Request.current.set(req);
        Lang.current.set(null);
        assertLocale(new Locale("en"));

        cookie = new Http.Cookie();
        cookie.name = "PLAY_LANG";
        cookie.value = "en";
        req.cookies.put(cookie.name, cookie);
        Http.Request.current.set(req);
        Lang.current.set(null);
        assertLocale(new Locale("en"));

        cookie = new Http.Cookie();
        cookie.name = "PLAY_LANG";
        cookie.value = "en_q";
        req.cookies.put(cookie.name, cookie);
        Http.Request.current.set(req);
        Lang.current.set(null);
        assertLocale(new Locale("en"));

        cookie = new Http.Cookie();
        cookie.name = "PLAY_LANG";
        cookie.value = "en_GB";
        req.cookies.put(cookie.name, cookie);
        Http.Request.current.set(req);
        Lang.current.set(null);
        assertLocale(new Locale("en", "GB"));


    }

    private void assertLocale(Locale locale) {
      assertThat(Lang.get()).isEqualTo(locale.toString());
      assertThat(Lang.getLocale()).isEqualTo(locale);
    }
}
