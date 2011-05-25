import org.junit.*;
import play.test.*;
import play.mvc.Http.*;
import models.*;
import controllers.*;

public class FunctionalTestTest extends FunctionalTest {
    
    @org.junit.Before
    public void setUp() throws Exception {
        Fixtures.deleteDatabase();
        Fixtures.loadModels("users.yml");
    }
    
    @Test
    public void testAndCall() {
        assertEquals(2, User.count());
        URL url = reverse(); {
            controllers.Users.newUser("Guillaume");
        }
        Response response = POST(url);
        assertIsOk(response);
        assertContentEquals("Created user with name Guillaume", response);
        assertEquals(3, User.count());
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
    
    @Test
    public void usingTransaction() {
        Response response = GET("/users/list");
        assertIsOk(response);
        assertContentEquals("2", response);
    }
    
    @Test
    public void usingTransaction2() {
        new User("Bob").create();
        Response response = GET("/users/list");
        assertIsOk(response);
        assertContentEquals("3", response);
        User bob = User.find("byName", "Bob").first();
        assertNotNull(bob);
    }
    
    @Test
    public void usingTransaction3() {
        Response response = POST("/users/newUser?name=Kiki");
        assertIsOk(response);
        assertEquals(3, User.count());
        User kiki = User.find("byName", "Kiki").first();
        assertNotNull(kiki);
    }
    
    public static class AnotherInnerTest extends UnitTest {
        
        @Test
        public void hello() {
            assertEquals(2, 1+1);
        }
        
    }
    
    @Test
    public void usingRedirection() {
        Response response = GET("/users/redirectToIndex");
        assertStatus( 302, response);
        String location = response.headers.get("Location").value();
        
        response = GET( location );
        assertIsOk(response);
        
        response = POST("/users/redirectToIndex");
        assertStatus( 302, response);
        location = response.headers.get("Location").value();
        
        response = POST( location );
        assertIsOk(response);
    }
    
}

