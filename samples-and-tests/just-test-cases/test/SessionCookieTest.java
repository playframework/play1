import org.junit.*;

import java.lang.reflect.*;
import play.*;
import play.test.*;
import play.mvc.*;
import play.mvc.Http.*;
import models.*;
import controllers.*;

public class SessionCookieTest extends FunctionalTest {

	@Test
    public void testSessionCookieMaxAge() throws InterruptedException {
    	Response response;
    	clearCookies();

    	changeMaxAgeConstant("1s");
    	
    	response = GET("/sessioncookie/put");
    	assertTrue(response.out.toString().contains("Yop"));

    	response = GET("/sessioncookie/index");
    	assertTrue("expected session data", response.out.toString().contains("Yop"));

    	Thread.sleep(1000);

    	response = GET("/sessioncookie/index");
    	assertFalse("session cookie should be expired", response.out.toString().contains("Yop"));

    	// Restore configuration
    	changeMaxAgeConstant(Play.configuration.getProperty("application.session.maxAge"));
    }

	private static void changeMaxAgeConstant(String maxAge) {
    	try {
	    	/*
	    	 * Set the final static value Scope.COOKIE_EXPIRE using reflection.
	    	 */
	        Field field = Scope.class.getField("COOKIE_EXPIRE");
	        field.setAccessible(true);
	        Field modifiersField = Field.class.getDeclaredField("modifiers");
	        modifiersField.setAccessible(true);
	        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

	        // Set the new value
	        field.set(null, maxAge);
    	} catch(Exception e) {
    		fail(e.getMessage());
    	}
    }
}
