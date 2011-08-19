import org.junit.Test;

import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.test.FunctionalTest;


public class ByteRangeServingTest extends FunctionalTest {
	@Test
	public void noByteRange() {
		HttpResponse resp = WS.url("http://localhost:9003/public/byterangeserving-testfile.txt").get();
		// check accept-range=bytes (all static contents should be served with this header now)
		assertEquals("bytes", resp.getHeader("accept-ranges"));
		assertEquals("0123456789", resp.getString());
	}
	
	@Test
	public void oneRange() {
		HttpResponse resp = WS.url("http://localhost:9003/public/byterangeserving-testfile.txt").setHeader("range", "bytes=0-3").get();
		assertEquals("0123", resp.getString());
		assertEquals("bytes", resp.getHeader("accept-ranges"));
		assertEquals("4", resp.getHeader("content-length"));
		assertEquals("bytes 0-3/10", resp.getHeader("content-range"));
		assertEquals("text/plain; charset=utf-8", resp.getContentType());
	}

	private static final String CRLF = "\r\n";
	private static final String line(String s) {
		return s + CRLF;
	}
	
	@Test
	public void multipart() {
		HttpResponse resp = WS.url("http://localhost:9003/public/byterangeserving-testfile.txt").setHeader("range", "bytes=0-3,6-7").get();
		Logger.info(resp.getHeader("content-type") + "=" + resp.getContentType());
		assertEquals("multipart/byteranges; boundary=$$$THIS_STRING_SEPARATES$$$", resp.getContentType());
		assertEquals("bytes", resp.getHeader("accept-ranges"));
		String r = resp.getString();
		
		String awaited = line("--$$$THIS_STRING_SEPARATES$$$") +
			line("Content-Type: text/plain; charset=utf-8") +
			line("ContentRange: bytes 0-3/10") + 
			CRLF +
			line("0123--$$$THIS_STRING_SEPARATES$$$") +
			line("Content-Type: text/plain; charset=utf-8") +
			line("ContentRange: bytes 6-7/10") +
			CRLF +
			"67";
		
		Logger.info("got response '%s'\nawaited: '%s'", r, awaited);
		assertEquals(awaited, r);
	}
}
