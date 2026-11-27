package play.test;

import jakarta.persistence.EntityManager;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import play.Invoker;
import play.Logger;
import play.Play;
import play.classloading.ApplicationClassloader;
import play.db.Configuration;
import play.db.jpa.JPA;

/**
 * JUnit 5 extension that provides Play lifecycle support for test classes.
 * <p>
 * Classloading and Play bootstrap are owned by {@link PlayLauncherSessionListener}:
 * it starts Play and installs {@link Play#classloader} as the thread context
 * classloader before JUnit Platform discovers tests. This extension validates that
 * invariant and wraps each test in a Play invocation.
 * <p>
 * The JUnit 4 runner wrapped the whole test statement (including {@code @Before},
 * the test method and {@code @After}) in {@code Invoker.invokeInThread}, so the JPA
 * plugin's filter started a transaction before {@code @Before} ran. JUnit 5 has no
 * equivalent "around the whole test" callback, so this extension reproduces the
 * same lifecycle explicitly: it starts a JPA transaction before user
 * {@code @BeforeEach} methods and commits it after user {@code @AfterEach}
 * methods. Without this, tests that use {@code Fixtures} in {@code @BeforeEach}
 * fail with "No active EntityManager ... transaction not started?" when they are
 * started outside an HTTP request (for example from an IDE).
 */
public class PlayJUnitExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback,
        TestExecutionExceptionHandler {

    public static final String invocationType = "JUnitTest";

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(PlayJUnitExtension.class);
    private static final String INVOCATION_STARTED = "invocationStarted";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        if (!(testClass.getClassLoader() instanceof ApplicationClassloader)) {
            throw new IllegalStateException("Play test class " + testClass.getName()
                    + " was loaded by " + testClass.getClassLoader()
                    + " instead of Play's ApplicationClassloader. "
                    + "Make sure PlayLauncherSessionListener is registered on the test classpath.");
        }
        TestEngine.initTest(testClass);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // Ensure the context classloader is set for this test thread in case it was reset by
        // the JUnit platform machinery or the test runs on a different thread.
        if (Play.classloader != null) {
            Thread.currentThread().setContextClassLoader(Play.classloader);
        }

        // Do not nest a Play invocation inside an already running one. This is the
        // case when tests are executed from an HTTP request (the test runner module)
        // or from another Play invocation: the surrounding invocation already owns
        // the transaction and the plugin before/after hooks.
        if (Invoker.InvocationContext.current() != null) {
            context.getStore(NAMESPACE).remove(INVOCATION_STARTED);
            return;
        }

        Invoker.InvocationContext.current.set(new Invoker.InvocationContext(invocationType));
        Play.pluginCollection.beforeInvocation();
        try {
            startTransactions();
        } catch (Throwable t) {
            Play.pluginCollection.afterInvocation();
            Invoker.InvocationContext.current.remove();
            throw t;
        }
        context.getStore(NAMESPACE).put(INVOCATION_STARTED, Boolean.TRUE);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (!Boolean.TRUE.equals(context.getStore(NAMESPACE).remove(INVOCATION_STARTED, Boolean.class))) {
            return;
        }
        try {
            if (context.getExecutionException().isPresent()) {
                markTransactionsForRollback();
            }
        } finally {
            try {
                Play.pluginCollection.afterInvocation();
            } finally {
                Invoker.InvocationContext.current.remove();
            }
        }
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        // Unwrap exceptions like in JUnit 4 runner
        Throwable root = ExceptionUtils.getRootCause(throwable);
        throw root != null ? root : throwable;
    }

    /**
     * Starts a transaction for every JPA persistence unit, mirroring the setup
     * part of {@link JPA#withTransaction(String, boolean, play.libs.F.Function0)}
     * (which is what {@link play.db.jpa.JPAPlugin.TransactionalFilter} uses for a
     * normal Play invocation).
     * <p>
     * Do not use {@link JPA#startTx(String, boolean)} here: it sets the Hibernate
     * flush mode to {@code COMMIT}, so a query executed after {@code persist()}
     * does not see the pending entity until the transaction commits.
     */
    private static void startTransactions() {
        for (String name : Configuration.getDbNames()) {
            if (JPA.isEnabled(name)) {
                EntityManager manager = JPA.newEntityManager(name);
                JPA.bindForCurrentThread(name, manager, false);
                manager.getTransaction().begin();
            }
        }
    }

    /**
     * Marks active transactions as rollback-only so that
     * {@link play.db.jpa.JPAPlugin#afterInvocation()} rolls them back instead of
     * committing a failed test's changes.
     */
    private static void markTransactionsForRollback() {
        for (String name : Configuration.getDbNames()) {
            try {
                if (JPA.isInsideTransaction(name)) {
                    JPA.setRollbackOnly(name);
                }
            } catch (RuntimeException e) {
                Logger.warn(e, "Could not mark JPA transaction '%s' as rollback-only", name);
            }
        }
    }
}
