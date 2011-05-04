import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.mvc.Http;
import play.mvc.Http.Cookie;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.test.FunctionalTest;


public class SessionTest extends FunctionalTest {
	
	public static Map<String, Http.Cookie> savedCookies = new HashMap<String, Http.Cookie>();
	
	public Request requestWithSavedCookies(Response response) {
		/*
		 * Workaround for lighthouse ticket [#794]
		 */
		Request request = newRequest();
		request.cookies = savedCookies;
		
		// FunctinalTest.savedCookies must be null
		clearCookies(); 
				
        for(Map.Entry<String,Http.Cookie> e : response.cookies.entrySet()) {
            if(e.getValue().maxAge == null || e.getValue().maxAge > 0) {
                savedCookies.put(e.getKey(), e.getValue());
            }
        }
		
		return request;
	}
	
	public static void clearSavedCookies() {
		savedCookies.clear();
	}

	public static void assertNoSessionCookie(Response response) {
		Cookie sessionCookie = response.cookies.get("PLAY_SESSION");
		assertNull(sessionCookie);
	}
	
	public static void assertSessionCookie(Response response) {
		Cookie sessionCookie = response.cookies.get("PLAY_SESSION");
		assertNotNull(sessionCookie);
	}
	
	@Before
	public void init() {
		clearSavedCookies();
	}
	
	@Test
	public void testSessionSendOnlyIfChanged() throws Exception {
		Request request;
		Response response;
		Cookie sessionCookie;

		// Set Scope.SESSION_SEND_ONLY_IF_CHANGED=true
		response = GET("/session/setSendOnlyIfChanged?value=true");	
		assertNoSessionCookie(response);
		
		// No session. No username.
		response = GET("/session/index");
		assertNoSessionCookie(response);
		assertTrue(response.out.toString().contains("No username is set!"));
		
		// Set a username and check the cookie
		response = GET("/session/put?key=username&value=Alice");
		assertSessionCookie(response);
		sessionCookie = response.cookies.get("PLAY_SESSION");		
		assertTrue(sessionCookie.value.contains("username"));
		assertTrue(sessionCookie.value.contains("Alice"));

		/* 
		 * Check if session is kept.
		 * As nothing changed no session in the response.
		 */
		request = requestWithSavedCookies(response);
		response = GET(request, "/session/index");
		assertNoSessionCookie(response);
		
		String content = response.out.toString();
		assertTrue(content.contains("A username is set in the session"));
		assertTrue(content.contains("username: Alice"));
	}
	
	@Test
	public void testThatAllChangesAreDetected() {
		Response response;
		
		// Set Scope.SESSION_SEND_ONLY_IF_CHANGED=true
		response = GET("/session/setSendOnlyIfChanged?value=true");	

		response = GET("/session/put?key=importantValue&value=42");	
		assertSessionCookie(response);
		
		response = GET("/session/remove?key=importantValue");	
		assertSessionCookie(response);
		
		response = GET("/session/index");	
		assertNoSessionCookie(response);
		
		response = GET("/session/removeKeys?keys=x&keys=y");	
		assertSessionCookie(response);
		
		response = GET("/session/clear");	
		assertSessionCookie(response);
	}
	
	@Test
	public void testDefautResendBehavior() throws Exception {
		Request request;
		Response response;
		Cookie sessionCookie;

		// False is the default right now.
		response = GET("/session/setSendOnlyIfChanged?value=false");	
		assertSessionCookie(response);
		
		response = GET("/session/index");
		assertSessionCookie(response);
		
		// Set 'username' to 'Alice'
		response = GET("/session/put?key=username&value=Alice");
		assertSessionCookie(response);

		// Session set correctly?
		sessionCookie = response.cookies.get("PLAY_SESSION");		
		assertTrue(sessionCookie.value.contains("username"));
		assertTrue(sessionCookie.value.contains("Alice"));

		// Is the session kept?
		request = requestWithSavedCookies(response);
		response = GET(request, "/session/index");
		String content = response.out.toString();
		
		assertTrue(content.contains("A username is set in the session"));
		assertTrue(content.contains("username: Alice"));
		
		// Session still in the response?
		assertSessionCookie(response);
		sessionCookie = response.cookies.get("PLAY_SESSION");		
		assertTrue(sessionCookie.value.contains("username"));
		assertTrue(sessionCookie.value.contains("Alice"));
	}
	
	
	@Test
	public void testResendAlways() {
		Response response;
		
		// Set Scope.SESSION_SEND_ONLY_IF_CHANGED=false
		response = GET("/session/setSendOnlyIfChanged?value=false");	
		assertSessionCookie(response);

		response = GET("/session/put?key=importantValue&value=42");	
		assertSessionCookie(response);
		
		response = GET("/session/remove?key=importantValue");	
		assertSessionCookie(response);
		
		response = GET("/session/index");	
		assertSessionCookie(response);
		
		response = GET("/session/removeKeys?keys=x&keys=y");	
		assertSessionCookie(response);
		
		response = GET("/session/clear");	
		assertSessionCookie(response);
	}
	
	@After
	public void restoreConfiguration() {
		GET("/session/restoreConfiguration");
	}
}
