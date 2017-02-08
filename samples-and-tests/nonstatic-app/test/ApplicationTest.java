import org.junit.Test;
import play.mvc.Http.Response;
import play.test.FunctionalTest;

public class ApplicationTest extends FunctionalTest {

    @Test
    public void indexPage() {
        Response response = GET("/");
        assertIsOk(response);
        assertContentType("text/html", response);
        assertCharset(play.Play.defaultWebEncoding, response);
        assertContentMatch("Welcome to the non-static world!", response);
    }
    
    @Test
    public void helloPage() {
        Response response = GET("/hello");
        assertIsOk(response);
        assertContentType("text/plain", response);
        assertCharset(play.Play.defaultWebEncoding, response);
        assertContentMatch("Hello world!", response);
    }
}