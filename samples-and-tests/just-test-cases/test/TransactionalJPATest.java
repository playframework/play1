import org.junit.Test;

import play.mvc.Http.Response;
import play.test.FunctionalTest;

public class TransactionalJPATest extends FunctionalTest {
    
    @Test
    public void testImport() {
        Response response = GET("/Transactional/readOnlyTest");
        assertIsOk(response);
        response = GET("/Transactional/echoHowManyPosts");
        assertIsOk(response);
        assertEquals("There are 0 posts", getContent(response));
    }
    
}

