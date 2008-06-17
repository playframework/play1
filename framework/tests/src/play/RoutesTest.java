package play;

import org.junit.BeforeClass;
import org.junit.Test;

public class RoutesTest extends TestSupport {
    
    static TestApp testApp;
    
    @BeforeClass
    public static void init() throws Exception {
        System.out.println("KIKI");
        testApp = createApp();
        testApp.addRoute("GET   /   Application.index");        
    }
    
    @Test
    public void testRootPath() {        
        System.out.println("Yop");
    }
    
    @Test
    public void dtestSimplePath() {
        System.out.println("Yop2");
    }

}
