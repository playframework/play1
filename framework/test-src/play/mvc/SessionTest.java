package play.mvc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.Test;

import play.Play;
import play.mvc.Http.Cookie;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Session;

public class SessionTest {

    private static void mockRequestAndResponse() {
        Request.current.set(new Request());
        Response.current.set(new Response());
    }

    public static void setSendOnlyIfChangedConstant(boolean value) {
        try {
            /*
             * Set the final static value Scope.SESSION_SEND_ONLY_IF_CHANGED using reflection.
             */
            Field field = Scope.class.getField("SESSION_SEND_ONLY_IF_CHANGED");
            field.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            // Set the new value
            field.setBoolean(null, value);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSessionManipulationMethods() {
        mockRequestAndResponse();
        Session session = Session.restore();
        assertFalse(session.changed);

        session.change();
        assertTrue(session.changed);

        // Reset
        session.changed = false;
        session.put("username", "Alice");
        assertTrue(session.changed);

        session.changed = false;
        session.remove("username");
        assertTrue(session.changed);

        session.changed = false;
        session.clear();
        assertTrue(session.changed);
    }

    @Test
    public void testSendOnlyIfChanged() {
        // Mock secret
        Play.secretKey = "0112358";

        Session session;
        setSendOnlyIfChangedConstant(true);
        mockRequestAndResponse();

        // Change nothing in the session
        session = Session.restore();
        session.put("username", "Bob");
        session.changed = false;
        session.save();
        assertNull(Response.current().cookies.get(Scope.COOKIE_PREFIX + "_SESSION"));

        mockRequestAndResponse();
        // Change the session
        session = Session.restore();
        session.put("username", "Bob");
        session.save();

        Cookie sessionCookie = Response.current().cookies.get(Scope.COOKIE_PREFIX + "_SESSION");
        assertNotNull(sessionCookie);
        assertTrue(sessionCookie.value.contains("username"));
        assertTrue(sessionCookie.value.contains("Bob"));
    }

    @Test
    public void testSendAlways() {
        Session session;
        setSendOnlyIfChangedConstant(false);

        mockRequestAndResponse();

        // Change nothing in the session
        session = Session.restore();
        session.put("username", "Bob");
        session.changed = false;
        session.save();
        assertNotNull(Response.current().cookies.get(Scope.COOKIE_PREFIX + "_SESSION"));
    }
    
    @Test
    public void testSendDeleteOnlyWhenCookieInRequest() {
        Session session;
        setSendOnlyIfChangedConstant(false);
        mockRequestAndResponse();
        
        // Change nothing in the session
        session = Session.restore();
        session.save();
        
        assertNull(Response.current().cookies.get(Scope.COOKIE_PREFIX + "_SESSION"));
    }

    @After
    public void restoreDefault() {
        final boolean SESSION_SEND_ONLY_IF_CHANGED = Play.configuration.getProperty("application.session.sendOnlyIfChanged", "false").toLowerCase().equals("true"); 
        setSendOnlyIfChangedConstant(SESSION_SEND_ONLY_IF_CHANGED);
    }
}