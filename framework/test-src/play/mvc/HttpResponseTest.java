package play.mvc;

import org.junit.Test;
import static org.fest.assertions.Assertions.assertThat;

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
        response.setCookie("testCookie", "testValue", "lax");
        assertThat(response.cookies.get("testCookie").sameSite).isEqualTo("lax");

        Http.Cookie.defaultDomain = ".abc.com";
        response = new Http.Response();
        response.setCookie("testCookie", "testValue", "strict");
        assertThat(response.cookies.get("testCookie").sameSite).isEqualTo("strict");
    }
}
