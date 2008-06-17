package play;

import java.io.File;
import java.io.IOException;
import play.test.ApplicationTest;

public abstract class TestSupport extends ApplicationTest {
    
    public static TestApp createApp() throws IOException {
        File root = new File(System.getProperty("tests-tmp")+"/app"+System.currentTimeMillis());
        root.mkdirs();
        new File(root, "app").mkdir();
        new File(root, "app/controllers").mkdir();
        new File(root, "app/models").mkdir();
        new File(root, "conf").mkdir();
        System.out.println(System.getProperty("tests-tmp"));
        return new TestApp(root);
    }
    
    public static class TestApp {
        
        File root;
        
        public TestApp(File root) {
            this.root = root;
        }
        
        public void addRoute(String definition) {
            
        }
        
    }

}
