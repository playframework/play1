package play.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import play.Play;
import play.Logger;

/**
 * Run application tests
 * ??
 */
public class TestEngine {

    public static void main(String[] args) {
        // TODO
    }

    static ExecutorService executor = Executors.newCachedThreadPool();

    public static List<Class> allSimpleTests() {
        return Play.classloader.getAssignableClasses(SimpleTest.class);
    }

    public static List<Class> allVirtualClientTests() {
        return Play.classloader.getAssignableClasses(VirtualClientTest.class);
    }

    public static TestResults run(final String name) {
        final TestResults testResults = new TestResults();

        try {
            // Load test class
            final Class testClass = Play.classloader.loadClass(name);

            // Simple test
            if(SimpleTest.class.isAssignableFrom(testClass)) {
                JUnitCore junit = new JUnitCore();
                junit.addListener(new Listener(testResults));
                junit.run(testClass);
            }

            // VirtualClient test
            if(VirtualClientTest.class.isAssignableFrom(testClass)) {
                Future<Result> futureResult = executor.submit(new Callable<Result>() {
                    public Result call() throws Exception {
                        JUnitCore junit = new JUnitCore();
                        junit.addListener(new Listener(testResults));
                        return junit.run(testClass);
                    }
                });
                try {
                    futureResult.get();
                } catch(Exception e) {
                    Logger.error("VirtualClient test has failed", e);
                }
            }
        } catch(ClassNotFoundException e) {
            Logger.error(e, "Test not found %s", name);
        }

        return testResults;
    }

    // ~~~~~~ Run listener
    static class Listener extends RunListener {
        
        TestResults results;
        TestResult current;
        
        public Listener(TestResults results) {
            this.results = results;
        }

        @Override
        public void testStarted(Description description) throws Exception {
            current = new TestResult();
            current.name = description.getDisplayName().substring(0,description.getDisplayName().indexOf("(") );
            current.time = System.currentTimeMillis();
        }

        @Override
        public void testFailure(Failure failure) throws Exception {
            current.error = failure.getMessage();
            current.passed = false;
            results.passed = false;
        }

        @Override
        public void testFinished(Description description) throws Exception {
            current.time = System.currentTimeMillis() - current.time;
            results.results.add(current);
        }
    }
    
    public static class TestResults {
        
        public List<TestResult> results = new ArrayList<TestResult>();
        public boolean passed = true;
        
    }
    
    public static class TestResult {
        public String name;
        public String error;
        public boolean passed = true;
        public long time;
    }
    
    
}
