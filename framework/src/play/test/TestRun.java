package play.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Computer;
import org.junit.runner.Request;

public class TestRun {

    private final List<Class> classes;

    TestRun() {
         classes = new ArrayList<>();
    }

    public static TestRun parse() {
        TestRun result = new TestRun();
        result.classes.addAll(TestEngine.allUnitTests());
        result.classes.addAll(TestEngine.allFunctionalTests());
        return result;
    }

    public Request createRequest(Computer computer) {
        return Request.classes(computer, classes.toArray(new Class<?>[0]));
    }
}
