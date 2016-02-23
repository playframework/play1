import static org.junit.Assert.assertEquals;

import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import play.libs.IO;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.ws.WSUrlFetch;
import play.test.UnitTest;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;


public class WSTest extends UnitTest {

    @Test
    public void multipleGetStreamOk() {
        String url = "http://google.com";
        HttpResponse response = WS.url(url).post();
        String resp1 = response.getString();
        // getString is repeatable
        String resp2 = response.getString();
        assertEquals(resp1, resp2);
    }

    @Test
    public void multipleGetStreamKo() {
        String url = "http://google.com";
        HttpResponse response = WS.url(url).post();
        String resp1 = IO.readContentAsString(response.getStream(), response.getEncoding());
        // Stream is consumed, no more content
        String resp2 = IO.readContentAsString(response.getStream(), response.getEncoding());
        assertNotEquals(resp1, resp2);
        assertEquals("", resp2);
    }

    @Test
    public void multiplePostContentTest2() {
        String url = "http://google.com";
        HttpResponse response = WS.url(url).post();

        InputStream is = response.getStream();
        String resp1 = IO.readContentAsString(is, response.getEncoding());

        try {
            is.reset();
        } catch (IOException e) {
        }

        String resp2 = IO.readContentAsString(is, response.getEncoding());

        assertEquals(resp1, resp2);
        assertNotEquals("", resp2);
    }

    @Test
    public void getWithVitualhostTest() {
        HttpResponse response = WS.url("http://74.125.204.100").withVirtualHost("www.google.com").get();
        assertNotNull(response);
        
        InputStream is = response.getStream();
        String resp1 = IO.readContentAsString(is, response.getEncoding());
        assertNotEquals("", resp1);
        assertTrue(resp1.contains("www.google.com"));
    }
}
