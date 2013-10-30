package play.mvc;

import com.ning.http.client.RequestBuilder;
import org.junit.Test;
import play.Play;

import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

public class RouterTest {

    @Test
    public void test_getBaseUrl() {

        Play.configuration = new Properties();

        // test with currentRequest
        Http.Request request = Http.Request.createRequest(
                null,
                "GET",
                "/",
                "",
                null,
                null,
                null,
                null,
                false,
                80,
                "localhost",
                false,
                null,
                null
        );

        Http.Request.current.set( request );
        assertThat(Router.getBaseUrl()).isEqualTo("http://localhost");

        // test without current request
        Http.Request.current.remove();
        // application.baseUrl without trailing /
        Play.configuration.setProperty("application.baseUrl", "http://a");
        assertThat(Router.getBaseUrl()).isEqualTo("http://a");

        // application.baseUrl with trailing /
        Play.configuration.setProperty("application.baseUrl", "http://b/");
        assertThat(Router.getBaseUrl()).isEqualTo("http://b");
    }
}
