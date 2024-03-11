package play.test;

import java.io.File;

import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import play.Play;
import play.test.junit.listeners.LoggingListener;
import play.test.junit.listeners.XMLReportListener;

public class Runner extends JUnitCore {

    public static void main(String... args) {
        Runner runner = new Runner();
        File root = new File(System.getProperty("application.path", "."));
        Play.init(root, System.getProperty("play.id", ""));
        Play.start();
        //noinspection ResultOfMethodCallIgnored
        new File("test-result").mkdirs();

        TestRun testRun = TestRun.parse(System.getProperty("tests", "all"));
        runner.addListener(new LoggingListener());
        runner.addListener(new XMLReportListener());
        Result result = runner.run(testRun.createRequest(defaultComputer()));
        System.exit(result.wasSuccessful() ? 0 : 1);
    }

    static Computer defaultComputer() {
        return new Computer();
    }
}
