package play.test;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Assert;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import play.Invoker.Invocation;
import play.Play;
import play.Logger;
import play.PlayPlugin;
import play.db.jpa.JPA;
import play.vfs.VirtualFile;

/**
 * Run application tests
 */
public class TestEngine {

    static ExecutorService functionalTestsExecutor = Executors.newSingleThreadExecutor();

    public static List<Class> allUnitTests() {
        List<Class> classes = Play.classloader.getAssignableClasses(Assert.class);
        for(ListIterator<Class> it = classes.listIterator(); it.hasNext(); ) {
            Class c = it.next();
            if(Modifier.isAbstract(c.getModifiers())) {
                it.remove();
            } else {
                if(FunctionalTest.class.isAssignableFrom(c)) {
                    it.remove();
                }
            }
        }
        return classes;
    }

    public static List<Class> allFunctionalTests() {
        List<Class> classes =  Play.classloader.getAssignableClasses(FunctionalTest.class);
        for(ListIterator<Class> it = classes.listIterator(); it.hasNext(); ) {
            if(Modifier.isAbstract(it.next().getModifiers())) {
                it.remove();
            }
        }
        return classes;
    }

    public static List<String> seleniumTests(String testPath, List<String> results) {
        File testDir = Play.getFile(testPath);
        if(testDir.exists()) {
            scanForSeleniumTests(testDir, results);
        }
        return results;
    }

    public static List<String> allSeleniumTests() {
        List<String> results = new ArrayList<String>();
        seleniumTests("test", results);
        for(VirtualFile root : Play.roots) {
            seleniumTests(root.relativePath()+"/test", results);
        }
        return results;
    }
    
    private static void scanForSeleniumTests(File dir, List<String> tests) {
        for(File f : dir.listFiles()) {
            if(f.isDirectory()) {
                scanForSeleniumTests(f, tests);
            } else if(f.getName().endsWith(".test.html")) {
                String test = f.getName();
                while(!f.getParentFile().getName().equals("test")) {
                    test = f.getParentFile().getName() + "/" + test;
                    f = f.getParentFile();
                }
                tests.add(test);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static TestResults run(final String name) {
        final TestResults testResults = new TestResults();

        try {
            // Load test class
            final Class testClass = Play.classloader.loadClass(name);

            for(PlayPlugin plugin : Play.plugins) {
                TestResults pluginTestResults = plugin.runTest(testClass);
                if(pluginTestResults != null) {
                    return pluginTestResults;
                }
            }

            // VirtualClient test
            if(FunctionalTest.class.isAssignableFrom(testClass)) {
                Future futureResult = functionalTestsExecutor.submit(new Invocation() {

                    @Override
                    public void execute() throws Exception {
                        JPA.em();
                        JUnitCore junit = new JUnitCore();
                        junit.addListener(new Listener(testClass.getName(), testResults));
                        junit.run(testClass);
                    }
                    
                });
                try {
                    futureResult.get();
                } catch(Exception e) {
                    Logger.error("VirtualClient test has failed", e);
                }
            } else {
                // Simple test
                JUnitCore junit = new JUnitCore();
                junit.addListener(new Listener(testClass.getName(), testResults));
                junit.run(testClass);
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
                    current.sourceFile = Play.classes.getApplicationClass(className).javaFile.relativePath();
                    current.sourceLine = stackTraceElement.getLineNumber();
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

        public void add(TestResult result) {
            this.results.add(result);
        }
        
    }
    
    public static class TestResult {
        public String name;
        public String error;
        public boolean passed = true;
        public long time;
        public String trace;
        public String sourceInfos;
        public String sourceCode;
        public String sourceFile;
        public int sourceLine;
    }
    
    
}
