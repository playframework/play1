import org.junit.Test;

import play.mvc.Http.Response;
import play.test.FunctionalTest;

public class LanguageTest extends FunctionalTest {
    
    @Test
    public void testLang() {
        Response response = GET("/language/lang");
        assertStatus(200, response);
        assertContentEquals("it_IT", response);
    }
}
