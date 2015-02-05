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
    public void multiplePosttContentTest() {
	String url = "http://google.com";
	HttpResponse response = WS.url(url).post();
	String resp1 = response.getString();
	// Stream is consumed, no more content
	String resp2 = response.getString();
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
}
