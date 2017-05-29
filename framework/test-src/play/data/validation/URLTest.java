package play.data.validation;

import org.junit.Test;
import play.i18n.MessagesBuilder;

import static org.junit.Assert.assertEquals;

public class URLTest {
    @Test
    public void validURLValidationTest() {
        new MessagesBuilder().build();
        Validation.current.set(new Validation());

        assertValidURL(true, "http://foo.com/blah_blah");
        assertValidURL(true, "http://foo.com/blah_blah/");
        assertValidURL(true, "http://foo.com/blah_blah_(wikipedia)");
        assertValidURL(true, "http://foo.com/blah_blah_(wikipedia)_(again)");
        assertValidURL(true, "http://www.example.com/wpstyle/?p=364");
        assertValidURL(true, "https://www.example.com/foo/?bar=baz&inga=42&quux");
        assertValidURL(true, "http://✪df.ws/123");
        assertValidURL(true, "http://userid:password@example.com:8080");
        assertValidURL(true, "http://userid:password@example.com:8080/");
        assertValidURL(true, "http://userid@example.com");
        assertValidURL(true, "http://userid@example.com/");
        assertValidURL(true, "http://userid@example.com:8080");
        assertValidURL(true, "http://userid@example.com:8080/");
        assertValidURL(true, "http://userid:password@example.com");
        assertValidURL(true, "http://userid:password@example.com/");
        assertValidURL(true, "http://142.42.1.1/");
        assertValidURL(true, "http://142.42.1.1:8080/");
        assertValidURL(true, "http://➡.ws/䨹");
        assertValidURL(true, "http://⌘.ws");
        assertValidURL(true, "http://⌘.ws/");
        assertValidURL(true, "http://foo.com/blah_(wikipedia)#cite-1");
        assertValidURL(true, "http://foo.com/blah_(wikipedia)_blah#cite-1");
        assertValidURL(true, "http://foo.com/unicode_(✪)_in_parens");
        assertValidURL(true, "http://foo.com/(something)?after=parens");
        assertValidURL(true, "http://☺.damowmow.com/");
        assertValidURL(true, "http://code.google.com/events/#&product=browser");
        assertValidURL(true, "http://j.mp");
        assertValidURL(true, "ftp://foo.bar/baz");
        assertValidURL(true, "http://foo.bar/?q=Test%20URL-encoded%20stuff");
        assertValidURL(true, "http://مثال.إختبار");
        assertValidURL(true, "http://例子.测试");
        assertValidURL(true, "http://उदाहरण.परीक्षा");
        assertValidURL(true, "http://-.~_!$&'()*+,;=:%40:80%2f::::::@example.com");
        assertValidURL(true, "http://1337.net");
        assertValidURL(true, "http://a.b-c.de");
        assertValidURL(true, "http://223.255.255.254");
    }

    /**
     * see ticket https://play.lighthouseapp.com/projects/57987-play-framework/tickets/1764-url-regex-is-not-correct
     */
    @Test
    public void validURL_1764_Test() {
        new MessagesBuilder().build();
        Validation.current.set(new Validation());

        assertValidURL(false, "http://localhost/");
        assertValidURL(false, "http://LOCALHOST/");
        assertValidURL(false, "http://localhost:80/");

        assertValidURL(true, "http://localhost/", false, true);
        assertValidURL(true, "http://LOCALHOST/", false, true);
        assertValidURL(true, "https://LOCALHOST/", false, true);
        assertValidURL(true, "ftp://LOCALHOST/", false, true);
        assertValidURL(true, "sftp://LOCALHOST/", false, true);
        assertValidURL(true, "rtsp://LOCALHOST/", false, true);
        assertValidURL(true, "mms://LOCALHOST/", false, true);
        assertValidURL(true, "http://localhost:80/", false, true);

        assertValidURL(false, "http://127.0.0.1");
        assertValidURL(false, "http://127.0.0.1:80");

        assertValidURL(true, "http://127.0.0.1", true, false);
        assertValidURL(true, "http://127.0.0.1:80", true, false);
    }

    private void assertValidURL(boolean valid, String url, boolean tldMandatory, boolean excludeLoopback) {
        Validation.clear();
        Validation.ValidationResult result = Validation.url("url", url, tldMandatory, excludeLoopback);
        assertEquals("Validation url [" + url + "] should be " + valid, valid, result.ok);
    }

    private void assertValidURL(Boolean valid, String url) {
        assertValidURL(valid, url, true, true);
    }
}
