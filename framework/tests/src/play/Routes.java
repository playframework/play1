package play;

import org.junit.BeforeClass;
import org.junit.Test;

public class Routes extends TestsSupport {
    
    TestApp testApp;
    
    @BeforeClass
    void init() throws Exception{
        testApp = createApp();
        testApp.addRoute("GET   /   Application.index");
    }
    
    @Test
    void testRootPath() {
        System.out.println("Yop");
    }

}
