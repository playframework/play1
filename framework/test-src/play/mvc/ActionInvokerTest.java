package play.mvc;

import org.junit.*;
import org.junit.Before;

import static org.junit.Assert.assertEquals;

public class ActionInvokerTest {
    private Object[] noArgs = new Object[0];

    @Before
    public void setUp() throws Exception {
        Http.Request.current.set(new Http.Request());
    }

    @Test
    public void invokeStaticJavaMethod() throws Exception {
        Http.Request.current().controllerClass = TestController.class;
        assertEquals("static", ActionInvoker.invokeControllerMethod(TestController.class.getMethod("staticJavaMethod"), noArgs));
    }

    @Test
    public void invokeNonStaticJavaMethod() throws Exception {
        Http.Request.current().controllerClass = TestController.class;
        assertEquals("non-static", ActionInvoker.invokeControllerMethod(TestController.class.getMethod("nonStaticJavaMethod"), noArgs));
    }

    @Test
    public void invokeScalaObjectMethod() throws Exception {
        Http.Request.current().controllerClass = TestScalaObject$.class;
        assertEquals("non-static", ActionInvoker.invokeControllerMethod(TestScalaObject$.class.getMethod("objectMethod"), noArgs));
    }

    @Test
    public void invokeScalaTraitMethod() throws Exception {
        Http.Request.current().controllerClass = TestScalaObject$.class;
        assertEquals("static-with-object", ActionInvoker.invokeControllerMethod(TestScalaTrait$class.class.getMethod("traitMethod", Object.class), new Object[] {null}));
    }

    public static class TestController extends Controller {
        public static String staticJavaMethod() {
            return "static";
        }

        public String nonStaticJavaMethod() {
            return "non-static";
        }
    }

    public static class TestScalaObject$ extends Controller {
        public static final TestScalaObject$ MODULE$ = new TestScalaObject$();

        public String objectMethod() {
            return "non-static";
        }

        @Override public String toString() {
            return "object";
        }
    }

    public abstract static class TestScalaTrait$class {
        public static String traitMethod(Object that) {
            return "static-with-" + that;
        }
    }
}
