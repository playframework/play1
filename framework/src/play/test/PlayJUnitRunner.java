package play.test;

import java.io.File;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.rules.MethodRule;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import play.Invoker;
import play.Invoker.DirectInvocation;
import play.Play;

public class PlayJUnitRunner extends Runner {

    public static final String invocationType = "JUnitTest";

    public static boolean useCustomRunner = false;
    
    // *******************
    JUnit4 jUnit4;

    public PlayJUnitRunner(Class testClass) throws ClassNotFoundException, InitializationError {
        synchronized (Play.class) {
            if (!Play.started) {
                Play.init(new File("."), PlayJUnitRunner.getPlayId());
                Play.javaPath.add(Play.getVirtualFile("test"));
                Play.start();
                useCustomRunner = true;
                Class classToRun = Play.classloader.loadApplicationClass(testClass.getName());
            }
            Class classToRun = Play.classloader.loadApplicationClass(testClass.getName());
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

    @Override
    public void run(final RunNotifier notifier) {
        jUnit4.run(notifier);
    }

    // *********************
    public enum StartPlay implements MethodRule {

        INVOKE_THE_TEST_IN_PLAY_CONTEXT {

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
