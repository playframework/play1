package play.utils;

import org.junit.Test;
import play.PlayBuilder;
import play.mvc.Http;

import static org.fest.assertions.Assertions.assertThat;

public class FakeRequestCreatorTest {
    @Test
    public void testCreateFakeRequestFromBaseUrl() throws Exception {

        new PlayBuilder().build();

        String url = "http://a.b.com/";
        Http.Request request = FakeRequestCreator.createFakeRequestFromBaseUrl( url );
        assertThat( request.secure).isFalse();
        assertThat( request.port).isEqualTo(80);
        assertThat( request.host).isEqualTo("a.b.com");
        assertThat( request.path).isEqualTo("/");

        url = "https://a.b.com";
        request = FakeRequestCreator.createFakeRequestFromBaseUrl( url );
        assertThat( request.secure).isTrue();
        assertThat( request.port).isEqualTo(443);
        assertThat( request.host).isEqualTo("a.b.com");
        assertThat( request.path).isEqualTo("/");

        url = "https://a.b.com/q";
        request = FakeRequestCreator.createFakeRequestFromBaseUrl( url );
        assertThat( request.secure).isTrue();
        assertThat( request.port).isEqualTo(443);
        assertThat( request.host).isEqualTo("a.b.com");
        assertThat( request.path).isEqualTo("/q");

        url = "http://a.b.com:90/q";
        request = FakeRequestCreator.createFakeRequestFromBaseUrl( url );
        assertThat( request.secure).isFalse();
        assertThat( request.port).isEqualTo(90);
        assertThat( request.host).isEqualTo("a.b.com");
        assertThat( request.path).isEqualTo("/q");

        url = "https://a.b.com:190/q";
        request = FakeRequestCreator.createFakeRequestFromBaseUrl( url );
        assertThat( request.secure).isTrue();
        assertThat( request.port).isEqualTo(190);
        assertThat( request.host).isEqualTo("a.b.com");
        assertThat( request.path).isEqualTo("/q");
    }
}
