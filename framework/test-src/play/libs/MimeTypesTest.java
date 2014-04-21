package play.libs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.Play;
import play.mvc.Http;
import play.mvc.Http.Response;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class MimeTypesTest {
  @Before
  public void setUp() {
    Play.configuration = new Properties();
    Http.Response.current.set(new Http.Response());
  }

  @After
  public void tearDown() {
    Http.Response.current.set(null);
  }

  @Test
    public void contentTypeShouldReturnResponseCharsetWhenAvailable() {
        Response.current().encoding = "my-response-encoding";
        assertEquals("text/xml; charset=my-response-encoding",
                     MimeTypes.getContentType("test.xml"));
    }

    @Test
    public void contentTypeShouldReturnDefaultCharsetInAbsenceOfResponse() {
        Response.current.set(null);
        assertEquals("text/xml; charset=" + play.Play.defaultWebEncoding,
            MimeTypes.getContentType("test.xml"));
    }
}
