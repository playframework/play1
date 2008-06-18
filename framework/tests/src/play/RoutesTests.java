package play;

import org.junit.BeforeClass;
import org.junit.Test;

public class RoutesTests extends TestsSupport {
    
    TestApp testApp;
    
    @BeforeClass
    public void init() throws Exception{
        testApp = createApp();
        testApp.addRoute("GET   /   Application.index");
        System.out.println("KIKI");
    }
    
    @Test
    public void testRootPath() {
        System.out.println("Yop");
    }

}
