import org.junit.*;
import play.test.*;
import play.mvc.*;
import play.mvc.Http.*;
import models.*;
import play.*;

public class ApplicationTest extends FunctionalTest {

    @Test
    public void testThatIndexPageWorks() {
        Response response = GET("/");
        assertIsOk(response);
        assertContentType("text/html", response);
        assertCharset(play.Play.defaultWebEncoding, response);
    }

    @Test
    public void testSimpleStatusCode() {
        Response response = GET("/application/simplestatuscode");
        assertStatus(204, response);
    }
    
    @Test
    public void testGettingUTF8FromConfig() {
        assertEquals("欢迎", Play.configuration.getProperty("utf8value"));
    }

}

