package play.mvc;

import org.junit.Test;
import static org.fest.assertions.Assertions.assertThat;

public class HttpResponseTest {

    @Test
    public void verifyDefaultCookieDomain() {
        Http.Cookie.defaultDomain = null;
        Http.Response response = new Http.Response();
        response.setCookie("testCookie", "testValue");
        assertThat(response.cookies.get("testCookie").domain).isNull();

        Http.Cookie.defaultDomain = ".abc.com";
        response = new Http.Response();
        response.setCookie("testCookie", "testValue");
        assertThat(response.cookies.get("testCookie").domain).isEqualTo(".abc.com");
    }
}
