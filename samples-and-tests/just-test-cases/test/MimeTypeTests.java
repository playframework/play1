import org.junit.Test;

import play.mvc.Http.Response;
import play.test.FunctionalTest;


public class MimeTypeTests extends FunctionalTest {

    @Test
    public void testDefaultMimeType() {
        Response response = GET("/public/images/favicon.png");
        assert(response.contentType.equals("image/png"));
    }

    @Test
    public void testNewMimeType() {
        Response response = GET("/public/thing.foobar");
        assert(response.contentType.equals("application/foobarstuff"));
    }

    @Test
    public void testOverridetMimeType() {
        Response response = GET("/public/image.gif");
        assert(response.contentType.equals("image/gifsucks"));
    }

}
