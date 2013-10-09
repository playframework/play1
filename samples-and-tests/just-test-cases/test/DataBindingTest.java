import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import play.libs.WS;
import play.mvc.Http;
import play.mvc.Http.Response;
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


    @Test
    public void testBindingList() {
        Http.Response response = POST("/DataBinding/myList?items[0].id=23&items[10].id=1&items[1].id=12&items[2].id=43&items[6].id=35&items[8].id=32", "text/plain", "A_body");

        assertIsOk(response);
        assertContentEquals("MyBook[23],MyBook[12],MyBook[43],null,null,null,MyBook[35],null,MyBook[32],null,MyBook[1]", response);
    }
    
    @Test
    public void testEditAnEntity() {
	Map<String, String> params = new HashMap<String, String>();
	params.put("entity.date", "2013-10-03 11:33:05:125 AM");
	params.put("entity.yop", "yop");
        Response response = POST(Router.reverse("DataBinding.editAnEntity").url, params);
        assertIsOk(response);
        assertContentMatch("2013-10-03 11:33:05:125 AM", response);
        assertContentMatch("--yop--", response);
    }

    @Test
    public void testDispatchAnEntity() {
	Map<String, String> params = new HashMap<String, String>();
	params.put("entity.date", "2013-10-03 11:33:05:125 AM");
	params.put("entity.yop", "yop");
        Response response = POST(Router.reverse("DataBinding.dispatchAnEntity").url, params);
        assertStatus(302, response);
    }
}

