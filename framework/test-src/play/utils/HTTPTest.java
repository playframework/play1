package play.utils;

import org.junit.Test;

import java.text.ParseException;
import java.util.Date;

import static org.apache.commons.lang.time.DateUtils.addDays;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HTTPTest {

    @Test
    public void testFixCaseForHttpHeader() {
        assertThat( HTTP.fixCaseForHttpHeader("Content-type")).isEqualTo("Content-Type");
        assertThat( HTTP.fixCaseForHttpHeader("Content-Type")).isEqualTo("Content-Type");
        assertThat( HTTP.fixCaseForHttpHeader("content-type")).isEqualTo("Content-Type");
        assertThat( HTTP.fixCaseForHttpHeader("Referer")).isEqualTo("Referer");
        assertThat( HTTP.fixCaseForHttpHeader("referer")).isEqualTo("Referer");
        // An one that is not in the list of valid http headers.
        String unknown = "Not-In-the-LiST";
        assertThat( HTTP.fixCaseForHttpHeader(unknown)).isEqualTo(unknown);
    }
    
    @Test
    public void testQuotedCharsetInHttpHeader() {

        HTTP.ContentTypeWithEncoding standardContentType = HTTP.parseContentType("text/html; charset=utf-8");
        assertThat(standardContentType.encoding).isEqualTo("utf-8");
        assertThat(standardContentType.contentType).isEqualTo("text/html");
        
        
        HTTP.ContentTypeWithEncoding doubleQuotedCharsetContentType = HTTP.parseContentType("text/html; charset=\"utf-8\"");
        assertThat(doubleQuotedCharsetContentType.encoding).isEqualTo("utf-8");
        assertThat(doubleQuotedCharsetContentType.contentType).isEqualTo("text/html");

        HTTP.ContentTypeWithEncoding simpleQuotedCharsetContentType = HTTP.parseContentType("text/html; charset='utf-8'");
        assertThat(simpleQuotedCharsetContentType.encoding).isEqualTo("utf-8");
        assertThat(simpleQuotedCharsetContentType.contentType).isEqualTo("text/html");

        HTTP.ContentTypeWithEncoding defaultContentType = HTTP.parseContentType(null);
        assertThat(defaultContentType.encoding).isEqualTo(null);
        assertThat(defaultContentType.contentType).isEqualTo("text/html");
    }

    @Test
    public void testIsModifiedMethod() throws ParseException {
        String etag = "6d82cbb050ddc7fa9cgb6590s4546d59";
        Date date = Utils.getHttpDateFormatter().parse("Thu, 16 Jan 2018 12:13:14 GMT");
        String unknown = "unknown";
        long lastModified = date.getTime();
        String ifModifiedSince = Utils.getHttpDateFormatter().format(date);
        String ifModifiedSinceOld = Utils.getHttpDateFormatter().format(addDays(date, -1));

        assertFalse(HTTP.isModified(etag, lastModified, etag, ifModifiedSince));
        assertFalse(HTTP.isModified(etag, 0, etag, ifModifiedSince));
        assertFalse(HTTP.isModified(etag, lastModified, null, ifModifiedSince));

        assertTrue(HTTP.isModified(etag, lastModified, unknown, ifModifiedSince));
        assertTrue(HTTP.isModified(etag, lastModified, "", ifModifiedSince));
        assertTrue(HTTP.isModified(etag, lastModified, null, null));

        assertTrue(HTTP.isModified(etag, lastModified, null, ifModifiedSinceOld));
        assertTrue(HTTP.isModified(etag, lastModified, null, ""));
        assertTrue(HTTP.isModified(etag, lastModified, null, null));
        assertTrue(HTTP.isModified(etag, lastModified, null, unknown));
    }
}
