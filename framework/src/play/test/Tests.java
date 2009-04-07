package play.test;

import java.util.List;
import play.Play;

public class Tests {

    public static List<Class> allApplicationTests() {
        return Play.classloader.getAssignableClasses(ApplicationTest.class);
    }

}
