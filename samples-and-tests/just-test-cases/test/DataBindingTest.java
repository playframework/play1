import org.junit.Test;
import play.libs.WS;
import play.mvc.Http;
import play.mvc.Router;
import play.test.FunctionalTest;
import play.test.UnitTest;

public class DataBindingTest extends FunctionalTest {

    @Test
    public void testThatBindingWithQueryStringAndBodyWorks() {
        Http.Response response = POST("/DataBinding/myInputStream?productCode=XXX", "text/plain", "A_body");

        assertIsOk(response);
        assertContentEquals("XXX - A_body", response);
    }



}

