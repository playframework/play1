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
}
