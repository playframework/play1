package play.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Assertions;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import play.Logger;
import play.Play;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.mvc.Scope.RenderArgs;
import play.vfs.VirtualFile;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Run application tests
 */
public class TestEngine {

    private static final class ClassNameComparator implements Comparator<Class> {
        @Override
        public int compare(Class aClass, Class bClass) {
            return aClass.getName().compareTo(bClass.getName());
        }
    }

    private static final ClassNameComparator classNameComparator = new ClassNameComparator();

    public static final ExecutorService functionalTestsExecutor = Executors.newSingleThreadExecutor();

    public static List<Class> allUnitTests() {
        List<Class> classes = new ArrayList<>();
        classes.addAll(Play.classloader.getAssignableClasses(Assertions.class));
        classes.addAll(Play.pluginCollection.getUnitTests());
        for (ListIterator<Class> it = classes.listIterator(); it.hasNext();) {
            Class c = it.next();
            if (Modifier.isAbstract(c.getModifiers())) {
                it.remove();
            } else {
                if (FunctionalTest.class.isAssignableFrom(c)) {
                    it.remove();
                }
            }
        }
        classes.sort(classNameComparator);
        return classes;
    }

    public static List<Class> allFunctionalTests() {
        List<Class> classes = new ArrayList<>();
        classes.addAll(Play.classloader.getAssignableClasses(FunctionalTest.class));
        classes.addAll(Play.pluginCollection.getFunctionalTests());

        classes.removeIf(aClass -> Modifier.isAbstract(aClass.getModifiers()));
        classes.sort(classNameComparator);
        return classes;
    }

    public static List<String> seleniumTests(String testPath, List<String> results) {
        File testDir = Play.getFile(testPath);
        if (testDir.exists()) {
            scanForSeleniumTests(testDir, results);
        }
        return results;
    }

    public static List<String> allSeleniumTests() {
        List<String> results = new ArrayList<>();
        seleniumTests("test", results);
        for (VirtualFile root : Play.roots) {
            seleniumTests(root.relativePath() + "/test", results);
        }
        Collections.sort(results);
        return results;
    }

    private static void scanForSeleniumTests(File dir, List<String> tests) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                scanForSeleniumTests(f, tests);
            } else if (f.getName().endsWith(".test.html")) {
                String test = f.getName();
                while (!f.getParentFile().getName().equals("test")) {
                    test = f.getParentFile().getName() + "/" + test;
                    f = f.getParentFile();
                }
                tests.add(test);
            }
        }
    }

    public static void initTest(Class<?> testClass) {
        CleanTest cleanTestAnnot = null;
        if (testClass != null) {
            cleanTestAnnot = testClass.getAnnotation(CleanTest.class);
        }
        if (cleanTestAnnot != null && cleanTestAnnot.removeCurrent() == true) {
            if (Request.current != null) {
                Request.current.remove();
            }
            if (Response.current != null) {
                Response.current.remove();
            }
            if (RenderArgs.current != null) {
                RenderArgs.current.remove();
            }
        }
        if (cleanTestAnnot == null || (cleanTestAnnot != null && cleanTestAnnot.createDefault() == true)) {
            if (Request.current() == null) {
                String host = Router.getBaseUrl();
                String domain = null;
                Integer port = 80;
                boolean isSecure = false;
                if (host == null || host.equals("application.baseUrl")) {
                    host = "localhost:" + port;
                    domain = "localhost";
                } else if (host.contains("http://")) {
                    host = host.replaceAll("http://", "");
                } else if (host.contains("https://")) {
                    host = host.replaceAll("https://", "");
                    port = 443;
                    isSecure = true;
                }
                int colonPos = host.indexOf(':');
                if (colonPos > -1) {
                    domain = host.substring(0, colonPos);
                    port = Integer.parseInt(host.substring(colonPos + 1));
                } else {
                    domain = host;
                }
                Request request = Request.createRequest(null, "GET", "/", "", null,
                        null, null, host, false, port, domain, isSecure, null, null);
                request.body = new ByteArrayInputStream(new byte[0]);
                Request.current.set(request);
            }

            if (Response.current() == null) {
                Response response = new Response();
                response.out = new ByteArrayOutputStream();
                response.direct = null;
                Response.current.set(response);
            }

            if (RenderArgs.current() == null) {
                RenderArgs renderArgs = new RenderArgs();
                RenderArgs.current.set(renderArgs);
            }
        }
    }

    public static TestResults run(String name) {
        TestResults testResults = new TestResults();

        try {
            Class testClass = Play.classloader.loadClass(name);

            initTest(testClass);

            TestResults pluginTestResults = Play.pluginCollection.runTest(testClass);
            if (pluginTestResults != null) {
                return pluginTestResults;
            }

            // Prepare JUnit 5 launcher request
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectClass(testClass))
                    .build();

            Launcher launcher = LauncherFactory.create();
            Listener listener = new Listener(testClass.getName(), testResults);

            launcher.registerTestExecutionListeners(listener);
            launcher.execute(request);

        } catch (ClassNotFoundException e) {
            Logger.error(e, "Test not found %s", name);
        }

        return testResults;
    }

    // ~~~~~~ Test Execution Listener (JUnit 5)
    static class Listener implements TestExecutionListener {

        final TestResults results;
        final String className;
        TestResult current;

        public Listener(String className, TestResults results) {
            this.results = results;
            this.className = className;
        }

        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            if (testIdentifier.isTest()) {
                current = new TestResult();
                current.name = testIdentifier.getDisplayName();
                current.time = System.currentTimeMillis();
            }
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            if (testIdentifier.isTest()) {
                current.time = System.currentTimeMillis() - current.time;
                TestExecutionResult.Status status = testExecutionResult.getStatus();
                if (status == TestExecutionResult.Status.SUCCESSFUL) {
                    current.passed = true;
                } else {
                    current.passed = false;
                    Throwable ex = testExecutionResult.getThrowable().orElse(null);
                    if (ex != null) {
                        if (ex instanceof AssertionError) {
                            current.error = "Failure, " + ex.getMessage();
                        } else {
                            current.error = "A " + ex.getClass().getName() + " has been caught, " + ex.getMessage();
                        }
                        current.trace = getStackTrace(ex);
                        for (StackTraceElement stackTraceElement : ex.getStackTrace()) {
                            if (stackTraceElement.getClassName().equals(className)) {
                                // Play.classes.getApplicationClass may need adaptation for source info
                                current.sourceInfos = "In " + Play.classes.getApplicationClass(className).javaFile.relativePath() + ", line " + stackTraceElement.getLineNumber();
                                String[] lines = Play.classes.getApplicationClass(className).javaSource.split("\n");
                                if (stackTraceElement.getLineNumber() - 1 < lines.length) {
                                    current.sourceCode = lines[stackTraceElement.getLineNumber() - 1];
                                }
                                current.sourceFile = Play.classes.getApplicationClass(className).javaFile.relativePath();
                                current.sourceLine = stackTraceElement.getLineNumber();
                            }
                        }
                    }
                }
                results.add(current);
            }
        }

        private String getStackTrace(Throwable throwable) {
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement element : throwable.getStackTrace()) {
                sb.append(element.toString()).append("\n");
            }
            return sb.toString();
        }
    }

    public static class TestResults {

        public List<TestResult> results = new ArrayList<>();
        public boolean passed = true;
        public int success = 0;
        public int errors = 0;
        public int failures = 0;
        public long time = 0;

        public void add(TestResult result) {
            time = result.time + time;
            this.results.add(result);
            if (result.passed) {
                success++;
            } else {
                if (result.error != null && result.error.startsWith("Failure")) {
                    failures++;
                } else {
                    errors++;
                }
                passed = false;
            }
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