package play.test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.runner.Computer;
import org.junit.runner.Request;

public class TestRun {

    private final List<Class> classes;

    TestRun() {
         classes = new ArrayList<>();
    }

    public static TestRun parse(String filer) {
        TestRun result = new TestRun();
        result.classes.addAll(TestEngine.allUnitTests());
        result.classes.addAll(TestEngine.allFunctionalTests());
        if (!"all".equals(filer) && filer != null && !filer.isEmpty()) {
            String[] split = filer.split(",");
            result.classes.removeIf(c -> Stream.of(split).noneMatch(prefix -> c.getName().startsWith(prefix)));
        }
        return result;
    }

    public Request createRequest(Computer computer) {
        return Request.classes(computer, classes.toArray(new Class<?>[0]));
    }
}
