import org.junit.*;
import play.test.*;
import play.mvc.Http.*;
import models.*;
import controllers.*;

public class FunctionalTestTest extends FunctionalTest {
    
    @org.junit.Before
    public void setUp() {
        Fixtures.deleteAll();
    }
    
    @Test
    public void testAndCall() {
        assertEquals(0, User.count());
        URL url = reverse(); {
            controllers.Users.newUser("Guillaume");
        }
        Response response = GET(url);
        assertIsOk(response);
        assertContentEquals("Created user with name Guillaume", response);
        assertEquals(1, User.count());
        User guillaume = User.find("byName", "Guillaume").first();
        assertNotNull(guillaume);
        assertEquals("Guillaume", guillaume.name);
    }

    @Test
    public void twoCalls() {
        Response response = GET("/jpacontroller/show");
        assertIsOk(response);
        response = GET("/jpacontroller/show");
        assertIsOk(response);
    }
    
    public static class AnotherInnerTest extends UnitTest {
        
        @Test
        public void hello() {
            assertEquals(2, 1+1);
        }
        
    }
    
}

