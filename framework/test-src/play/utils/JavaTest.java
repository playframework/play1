package play.utils;

import org.junit.Test;
import play.PlayBuilder;
import play.mvc.After;
import play.mvc.Before;
import play.mvc.Finally;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 2/6/11
 * Time: 12:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class JavaTest {

    @org.junit.Before
    public void playBuilderBefore() {
        new PlayBuilder().build();
    }

    private static class ActionClass {

        private static String privateMethod() {
            return "private";
        }


        public static String actionMethod() {
            return "actionMethod";
        }

        @Before
        public static String beforeMethod() {
            return "before";
        }

        @After
        public static String afterMethod() {
            return "after";
        }

        @Finally
        public static String finallyMethod() {
            return "finally";
        }

    }

    private static class ActionClassChild extends ActionClass {

    }

    @Test
    public void testFindActionMethod() throws Exception {
        assertNull(Java.findActionMethod("noneExistingMethod", ActionClass.class));
        assertNull(Java.findActionMethod("privateMethod", ActionClass.class));
        assertNull(Java.findActionMethod("beforeMethod", ActionClass.class));
        assertNull(Java.findActionMethod("afterMethod", ActionClass.class));
        assertNull(Java.findActionMethod("finallyMethod", ActionClass.class));

        Method m = Java.findActionMethod("actionMethod", ActionClass.class);
        assertNotNull( m );
        assertEquals("actionMethod", m.invoke( new ActionClass()));

        //test that it works with subclassing
        m = Java.findActionMethod("actionMethod", ActionClassChild.class);
        assertNotNull( m );
        assertEquals("actionMethod", m.invoke( new ActionClassChild()));

    }
}
