package play;

import java.io.File;
import java.io.IOException;
import play.libs.Files;
import play.test.ApplicationTest;

public abstract class TestSupport extends ApplicationTest {
    
    public static void start(TestApp app) {
        start(app.root);
    }
    
    public static TestApp createApp() throws IOException {
        File root = new File(System.getProperty("tests-tmp")+"/app"+System.currentTimeMillis());
        root.mkdirs();
        new File(root, "app").mkdir();
        new File(root, "app/controllers").mkdir();
        new File(root, "app/models").mkdir();
        new File(root, "conf").mkdir();
        Files.writeContent("application.name=Test", new File(root, "conf/application.conf"));
        Files.writeContent("# Routes", new File(root, "conf/routes"));
        return new TestApp(root);
    }
    
    public static class TestApp {
        
        File root;
        
        public TestApp(File root) {
            this.root = root;
        }
        
        public void addRoute(String definition) throws IOException {
            Files.appendContent(definition, new File(root, "conf/routes"));
        }

        public void writeController(String name, String code) throws IOException  {
            Files.writeContent(code, new File(root, "app/controllers/"+name+".java"));
        }

        public void createControllerPackage(String string) {
            new File(root, "app/controllers/"+string).mkdirs();
        }
        
    }

}
