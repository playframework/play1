package play.libs;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import play.PlayBuilder;
import play.libs.MimeTypes;
import play.mvc.Http.Response;


/**
 * Tests for {@link MimeTypes} class.
 */
public class MimeTypesTest {
    @BeforeClass
    public static void setUp() {
        PlayBuilder playBuilder = new PlayBuilder();
        playBuilder.build();
        playBuilder.initMvcObject();
      }
    
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
