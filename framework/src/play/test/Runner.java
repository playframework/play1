package play.test;

import java.io.File;

import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import play.Play;

public class Runner extends JUnitCore {

    public static void main(String... args) {
        Runner runner = new Runner();
        File root = new File(System.getProperty("application.path", "."));
        Play.init(root, System.getProperty("play.id", ""));
        Play.start();

        TestRun testRun = TestRun.parse();
        runner.addListener(LoggingListener.INSTANCE);
        Result result = runner.run(testRun.createRequest(defaultComputer()));
        System.exit(result.wasSuccessful() ? 0 : 1);
    }

    static Computer defaultComputer() {
        return new Computer();
    }
}
