package play.mvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import play.Play;
import play.libs.Codec;

public class HttpRequestTest {

    @Test
    public void testBasicAuth() {

        Play.configuration = new Properties();

        String encoded = Codec.encodeBASE64("username:pass:wordwithcolon");
        Http.Header header = new Http.Header("authorization", "Basic "+encoded);
        Map<String, Http.Header> headers = new HashMap<>();
        headers.put("authorization", header);

        //This used to throw an exception if there was a colon in the password
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
                headers,
                null
        );
    }
}
