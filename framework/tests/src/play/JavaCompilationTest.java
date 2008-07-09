package play;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import play.TestSupport.TestApp;
import play.exceptions.JavaCompilationException;

public class JavaCompilationTest extends TestSupport {

    static TestApp testApp;

    @BeforeClass
    public static void init() throws Exception {
        testApp = createApp();
        testApp.addRoute("GET   /                               Application.index");
        start(testApp);
    }

    @AfterClass
    public static void end() {
        stop();
    }

    @Test
    public void compilationError() throws Exception {
        testApp.writeController("Application",
                "package controllers;\n" +
                "public class Application extends play.mvc.Controller {\n" +
                "   public static void index() {\n" +
                "       renderText(\"Index\")\n" +
                "   }\n" +
                "}\n");
        try {
            Play.start();
        } catch (JavaCompilationException e) {
            assertFalse("Play is started !!", Play.started);
            assertEquals((int) 4, (int) e.getLineNumber());
            testApp.removeController("Application");
            return;
        }
        fail("No java compilation exception ???");
    }

    @Test
    public void fixCompilationError() throws Exception {
        Thread.sleep(1000);
        testApp.writeController("Application",
                "package controllers;" +
                "public class Application extends play.mvc.Controller {" +
                "   public static void index() {" +
                "       renderText(\"Index\");" +
                "   }" +
                "}");
        Play.detectChanges();
        assertTrue("Play is not started !!", Play.started);
    }
}
