import org.junit.*;
import play.test.*;
import play.mvc.*;
import play.mvc.Http.*;
import models.*;

public class FunctionalTestTest extends FunctionalTest {

    @Test
    public void twoCalls() {
        Response response = GET("/jpacontroller/show");
        assertIsOk(response);
        response = GET("/jpacontroller/show");
        assertIsOk(response);
    }
    
}

