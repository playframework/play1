package play;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import play.libs.IO;
import play.test.ApplicationTest;

public abstract class TestSupport extends ApplicationTest {
    
    public static void start(TestApp app) {
        Play.init(app.root, "test");
    }
    
    public static void stop() {
        Play.stop();
    }
    
    public static TestApp createApp() throws IOException {
        File root = new File(System.getProperty("tests-tmp")+"/app"+System.currentTimeMillis());
        root.mkdirs();
        new File(root, "app").mkdir();
        new File(root, "app/controllers").mkdir();
        new File(root, "app/models").mkdir();
        new File(root, "conf").mkdir();
        IO.writeContent("application.name=Test", new FileOutputStream(new File(root, "conf/application.conf")));
        IO.writeContent("# Routes", new FileOutputStream(new File(root, "conf/routes")));
        return new TestApp(root);
    }
    
    public static class TestApp {
        
        File root;
        
        public TestApp(File root) {
            this.root = root;
        }
        
        public void addRoute(String definition) throws IOException {
            IO.writeContent(definition, new FileOutputStream(new File(root, "conf/routes"), true));
        }

        public void writeController(String name, String code) throws IOException  {
            IO.writeContent(code, new FileOutputStream(new File(root, "app/controllers/"+name+".java")));
        }

        public void createControllerPackage(String string) {
            new File(root, "app/controllers/"+string).mkdirs();
        }
        
    }

}
