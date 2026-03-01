package play.test;

import java.io.File;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import play.Invoker;
import play.Invoker.DirectInvocation;
import play.Play;
import java.lang.reflect.Method;

public class PlayJUnitExtension implements BeforeAllCallback, BeforeEachCallback, InvocationInterceptor, TestExecutionExceptionHandler {

    public static final String invocationType = "JUnitTest";

    public static boolean useCustomRunner = false;

    private static final ThreadLocal<Invoker.InvocationContext> invocationContext = ThreadLocal.withInitial(() -> null);

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        synchronized (Play.class) {
            if (!Play.started) {
                Play.init(new File("."), getPlayId());
                Play.javaPath.add(Play.getVirtualFile("test"));
                if (!Play.started) {
                    Play.start();
                }
                useCustomRunner = true;
            }
        }
        // Initialize Play for the test class if needed (equivalent to initTest in old code)
        Class<?> testClass = context.getRequiredTestClass();
        TestEngine.initTest(testClass);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (useCustomRunner) {
            if (!Play.started) {
                Play.forceProd = true;
                Play.init(new File("."), getPlayId());
            }
            // Store invocation context in ThreadLocal for use in interceptTestMethod
            invocationContext.set(new Invoker.InvocationContext(invocationType));
        }
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        if (useCustomRunner) {
            // Run the test method within the Play thread context
            Invoker.InvocationContext ctx = new Invoker.InvocationContext(invocationType);
            Throwable[] testException = new Throwable[1];
            
            Invoker.invokeInThread(new DirectInvocation() {
                @Override
                public void execute() throws Exception {
                    try {
                        invocation.proceed();
                    } catch (Throwable e) {
                        testException[0] = e;
                    }
                }
                
                @Override
                public Invoker.InvocationContext getInvocationContext() {
                    return ctx;
                }
            });
            
            if (testException[0] != null) {
                throw testException[0];
            }
        } else {
            invocation.proceed();
        }
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        // Unwrap exceptions like in JUnit 4 runner
        Throwable root = ExceptionUtils.getRootCause(throwable);
        throw root != null ? root : throwable;
    }

    private static String getPlayId() {
        String playId = System.getProperty("play.id", "test");
        if(! (playId.startsWith("test-") && playId.length() >= 6)) {
            playId = "test";
        }
        return playId;
    }

}