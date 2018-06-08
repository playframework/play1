package play.test;

import java.io.File;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.rules.MethodRule;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import play.Invoker;
import play.Invoker.DirectInvocation;
import play.Play;

public class PlayJUnitRunner extends Runner implements Filterable {

    public static final String invocationType = "JUnitTest";

    public static boolean useCustomRunner = false;
    
    // *******************
    JUnit4 jUnit4;

    public PlayJUnitRunner(Class testClass) throws ClassNotFoundException, InitializationError {
        synchronized (Play.class) {
            if (!Play.started) {
                Play.init(new File("."), PlayJUnitRunner.getPlayId());
                Play.javaPath.add(Play.getVirtualFile("test"));
                // Assure that Play is not start (start can be called in the Play.init method)
                if (!Play.started) {
                    Play.start();
                }
                useCustomRunner = true;
            }
            Class<?> classToRun = Play.classloader.loadApplicationClass(testClass.getName());
            jUnit4 = new JUnit4(classToRun);
        }
    }

    private static String getPlayId() {
        String playId = System.getProperty("play.id", "test");
        if(! (playId.startsWith("test-") && playId.length() >= 6)) {
            playId = "test";
        }
        return playId;
    }

    @Override
    public Description getDescription() {
        return jUnit4.getDescription();
    }
    
    private void initTest() {
        TestClass testClass = jUnit4.getTestClass();
        if(testClass != null){
            TestEngine.initTest(testClass.getJavaClass());
        }
    }

    @Override
    public void run(RunNotifier notifier) {
        initTest();
        jUnit4.run(notifier);
    }

    @Override
    public void filter(Filter toFilter) throws NoTestsRemainException {
        jUnit4.filter(toFilter);

    }

    // *********************
    public enum StartPlay implements MethodRule {

        INVOKE_THE_TEST_IN_PLAY_CONTEXT {

            @Override
            public Statement apply(final Statement base, FrameworkMethod method, Object target) {

                return new Statement() {

                    @Override
                    public void evaluate() throws Throwable {
                        if (!Play.started) {
                            Play.forceProd = true;
                            Play.init(new File("."), PlayJUnitRunner.getPlayId());
                        }

                        try {
                            Invoker.invokeInThread(new DirectInvocation() {

                                @Override
                                public void execute() throws Exception {
                                    try {
                                        base.evaluate();
                                    } catch (Throwable e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                @Override
                                public Invoker.InvocationContext getInvocationContext() {
                                    return new Invoker.InvocationContext(invocationType);
                                }
                            });
                        } catch (Throwable e) {
                            throw ExceptionUtils.getRootCause(e);
                        }
                    }
                };
            }
        },
        JUST_RUN_THE_TEST {

            @Override
            public Statement apply(final Statement base, FrameworkMethod method, Object target) {
                return new Statement() {

                    @Override
                    public void evaluate() throws Throwable {
                        base.evaluate();
                    }
                };
            }
        };

        public static StartPlay rule() {
            return PlayJUnitRunner.useCustomRunner ? INVOKE_THE_TEST_IN_PLAY_CONTEXT : JUST_RUN_THE_TEST;
        }
    }
}
