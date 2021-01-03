package play.test;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import org.junit.runner.Computer;
import org.junit.runner.Description;
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
        new File("test-result").mkdirs();

        TestRun testRun = TestRun.parse();
        runner.addListener(LoggingListener.INSTANCE);
        runner.addListener(new XMLReportListener(new XMLJUnitResultFormatter()) {
            @Override
            public void testStarted(Description description) throws Exception {
                formatter.setOutput(new FileOutputStream(new File("test-result", "TEST-" + description.getClassName() + "-" + description.getMethodName() + ".xml")));
                super.testStarted(description);
            }
        });
        Result result = runner.run(testRun.createRequest(defaultComputer()));
        System.exit(result.wasSuccessful() ? 0 : 1);
    }

    static Computer defaultComputer() {
        return new Computer();
    }
}
