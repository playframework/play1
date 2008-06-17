package play.test;

import java.io.File;
import junit.framework.TestCase;
import play.Play;
import play.mvc.Http.Response;

public abstract class ApplicationTest {
    
    public void start(File root) {
        Play.init(root);
        Play.start();
    }
    
    public void stop() {
        Play.stop();
    }
    
    public Response GET(String path) {
        return null;
    }

}
