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

    public static List<Class> allUnitTests() {
        return Play.classloader.getAssignableClasses(UnitTest.class);
    }

    public static List<Class> allFunctionalTests() {
        return Play.classloader.getAssignableClasses(FunctionalTest.class);
    }

    public static TestResults run(final String name) {
        final TestResults testResults = new TestResults();

        try {
            // Load test class
            final Class testClass = Play.classloader.loadClass(name);

            // Simple test
            if(UnitTest.class.isAssignableFrom(testClass)) {
                JUnitCore junit = new JUnitCore();
                junit.addListener(new Listener(testClass.getName(), testResults));
                junit.run(testClass);
            }

            // VirtualClient test
            if(FunctionalTest.class.isAssignableFrom(testClass)) {
                Future<Result> futureResult = executor.submit(new Callable<Result>() {
                    public Result call() throws Exception {
                        JUnitCore junit = new JUnitCore();
                        junit.addListener(new Listener(testClass.getName(), testResults));
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
        String className;
        
        public Listener(String className, TestResults results) {
            this.results = results;
            this.className = className;
        }

        @Override
        public void testStarted(Description description) throws Exception {
            current = new TestResult();
            current.name = description.getDisplayName().substring(0,description.getDisplayName().indexOf("(") );
            current.time = System.currentTimeMillis();
        }

        @Override
        public void testFailure(Failure failure) throws Exception {
            if(failure.getException() instanceof AssertionError) {
                current.error = "Failure, " + failure.getMessage();
            } else {
                current.error = "A " + failure.getException().getClass().getName() + " has been caught, " + failure.getMessage();
                current.trace = failure.getTrace();
            }
            for(StackTraceElement stackTraceElement : failure.getException().getStackTrace()) {
                if(stackTraceElement.getClassName().equals(className)) {
                    current.sourceInfos = "In " + Play.classes.getApplicationClass(className).javaFile.relativePath() + ", line " + stackTraceElement.getLineNumber();
                    current.sourceCode = Play.classes.getApplicationClass(className).javaSource.split("\n")[stackTraceElement.getLineNumber()-1];
                }
            }
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
        public String trace;
        public String sourceInfos;
        public String sourceCode;
    }
    
    
}
