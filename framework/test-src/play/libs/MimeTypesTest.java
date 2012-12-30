package play.libs;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import play.mvc.Http.Response;


/**
 * Tests for {@link MimeTypes} class.
 */
public class MimeTypesTest {
    @Test
    public void contentTypeShouldReturnResponseCharsetWhenAvailable() throws Exception {
        String oldEncoding = Response.current().encoding;
        try {
            Response.current().encoding = "my-response-encoding";
            assertEquals("text/xml; charset=my-response-encoding",
                         MimeTypes.getContentType("test.xml"));
        }
        finally {
            Response.current().encoding = oldEncoding;
        }
    }

    @Test
    public void contentTypeShouldReturnDefaultCharsetInAbsenceOfResponse() throws Exception {
        Response originalResponse = Response.current();
        try {
            Response.current.set(null);
            assertEquals("text/xml; charset=" + play.Play.defaultWebEncoding,
                         MimeTypes.getContentType("test.xml"));
        }
        finally {
            Response.current.set(originalResponse);
        }
    }
}
