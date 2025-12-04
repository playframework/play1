package play.mvc;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class HttpResponseTest {

    @Test
    public void verifyDefaultCookieDomain() {
        Http.Cookie.defaultDomain = null;
        Http.Response response = new Http.Response();
        response.setCookie("testCookie", "testValue", null);
        assertThat(response.cookies.get("testCookie").domain).isNull();

        Http.Cookie.defaultDomain = ".abc.com";
        response = new Http.Response();
        response.setCookie("testCookie", "testValue", null);
        assertThat(response.cookies.get("testCookie").domain).isEqualTo(".abc.com");
    }

    @Test
    public void verifySameSiteCookie() {
        Http.Cookie.defaultDomain = null;
        Http.Response response = new Http.Response();
        response.setCookie("testCookie", "testValue", null);
        assertThat(response.cookies.get("testCookie").sameSite).isNull();

        Http.Cookie.defaultDomain = ".abc.com";
        response = new Http.Response();
        response.setCookie("testCookie", "testValue", Http.SameSite.LAX);
        assertThat(response.cookies.get("testCookie").sameSite).isEqualTo(Http.SameSite.LAX);

        Http.Cookie.defaultDomain = ".abc.com";
        response = new Http.Response();
        response.setCookie("testCookie", "testValue", Http.SameSite.STRICT);
        assertThat(response.cookies.get("testCookie").sameSite).isEqualTo(Http.SameSite.STRICT);
    }
}
