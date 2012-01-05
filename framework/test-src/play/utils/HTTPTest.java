package play.utils;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

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
}
