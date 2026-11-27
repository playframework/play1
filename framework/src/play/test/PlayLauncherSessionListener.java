package play.test;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import play.Logger;
import play.Play;
import play.server.Server;
import play.vfs.VirtualFile;

/**
 * Bootstraps Play and exposes its {@link play.classloading.ApplicationClassloader}
 * as the thread context classloader for the duration of a JUnit Platform session.
 * <p>
 * JUnit Platform launchers (IntelliJ, Eclipse, Maven Surefire, Gradle, Ant, the
 * console launcher) create discovery selectors from class names and resolve them
 * through {@code Thread.currentThread().getContextClassLoader()} (see JUnit's
 * {@code ClassLoaderUtils.getDefaultClassLoader()}). By installing Play's
 * application classloader before discovery, Jupiter loads the test class and,
 * transitively, all application classes through Play, so bytecode enhancers
 * (controllers, continuations, mailers, signatures, local variable names) are
 * applied even when tests are started from an IDE.
 * <p>
 * The listener is registered through
 * {@code META-INF/services/org.junit.platform.launcher.LauncherSessionListener}
 * and is auto-discovered by the JUnit Platform, so no launcher configuration,
 * system property or custom {@code TestEngine} is required.
 * <p>
 * For Play applications it also starts an in-process HTTP server when no server
 * is already listening on the configured port. Tests that use {@link play.libs.WS}
 * against {@code http://localhost:9003} require a real server; Play's own test
 * runner provides one, and an IDE/Gradle/Maven run does not. A server started
 * this way uses daemon Netty threads and is stopped when the launcher session
 * closes.
 * <p>
 * The application root is taken from the {@code application.path} system
 * property when it is set (the same property Play's server uses), otherwise from
 * the working directory. When that directory is not a Play application (no
 * {@code conf/application.conf}), the listener does nothing. This keeps the
 * framework's own JUnit tests and non-Play projects that depend on Play working
 * normally.
 */
public class PlayLauncherSessionListener implements LauncherSessionListener {

    private static final ThreadLocal<Deque<ClassLoader>> previousClassLoaders =
            ThreadLocal.withInitial(ArrayDeque::new);

    private static Server startedServer;

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        if (!tryStartPlay()) {
            return;
        }
        // A server that is already listening means the tests run inside Play's own
        // test runner or against a running application; keep its dev-mode reloading.
        // Otherwise this session owns the application lifecycle (IDE, Gradle, Maven).
        boolean disableChangesDetection = !isServerListening(getHttpPort());
        if (disableChangesDetection) {
            // Pick up sources changed since a previous test session in the same JVM
            // (before the session's test classes are discovered), then freeze the
            // classloader for the rest of the session.
            Play.detectChanges();
        }
        startServerIfNeeded();
        previousClassLoaders.get().push(previous);
        Thread.currentThread().setContextClassLoader(Play.classloader);
    }

    @Override
    public void launcherSessionClosed(LauncherSession session) {
        stopServer();
        Deque<ClassLoader> stack = previousClassLoaders.get();
        if (stack.isEmpty()) {
            previousClassLoaders.remove();
            return;
        }
        Thread.currentThread().setContextClassLoader(stack.pop());
        if (stack.isEmpty()) {
            previousClassLoaders.remove();
        }
    }

    /**
     * Starts Play if the resolved application root is a Play application.
     *
     * @return {@code true} if Play is initialized, {@code false} if this is not a Play application
     */
    static synchronized boolean tryStartPlay() {
        if (Play.started) {
            addTestSourcePath();
            return true;
        }

        File applicationRoot = findApplicationRoot();
        if (applicationRoot == null) {
            return false;
        }

        if (!Play.initialized) {
            Play.init(applicationRoot, getPlayId());
        }

        if (!Play.started) {
            Play.start();
        }

        // Add the test source path only after start(), so plugins (such as the
        // test runner module) have already registered their own test roots and we
        // do not add a duplicate entry.
        addTestSourcePath();
        Thread.currentThread().setContextClassLoader(Play.classloader);
        return true;
    }

    /**
     * Starts Play's HTTP server when tests are launched outside Play's own test
     * runner (IDE, Gradle, Maven, ...). Some tests use {@link play.libs.WS} to
     * call the application over real HTTP and therefore need a listening server,
     * exactly like {@code play test} provides.
     * <p>
     * When a server is already listening on the configured port (for example
     * because the tests run inside {@code play test}), it is left untouched and
     * will not be stopped when this launcher session ends.
     */
    private static synchronized void startServerIfNeeded() {
        if (startedServer != null) {
            return;
        }
        int port = getHttpPort();
        if (port <= 0 || isServerListening(port)) {
            return;
        }
        try {
            startedServer = new Server(new String[0], runnable -> {
                Thread thread = new Thread(runnable, "play-test-server");
                thread.setDaemon(true);
                return thread;
            });
        } catch (Throwable e) {
            Logger.error(e, "Could not start Play HTTP server on port %s for tests", port);
        }
    }

    private static synchronized void stopServer() {
        if (startedServer == null) {
            return;
        }
        try {
            startedServer.stop();
        } catch (Throwable e) {
            Logger.warn(e, "Could not stop Play HTTP server after tests");
        } finally {
            startedServer = null;
        }
    }

    /**
     * Mirrors the default port calculation of {@link Server}.
     */
    private static int getHttpPort() {
        int httpPort = parseInt(Play.configuration.getProperty("http.port", "-1"));
        int httpsPort = parseInt(Play.configuration.getProperty("https.port", "-1"));
        if (httpPort == -1 && httpsPort == -1) {
            return 9000;
        }
        return httpPort;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException e) {
            return -1;
        }
    }

    private static boolean isServerListening(int port) {
        if (port <= 0) {
            return false;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(getHttpAddress(), port), 200);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static InetAddress getHttpAddress() {
        String address = Play.configuration.getProperty("http.address");
        if (address == null || address.trim().isEmpty()) {
            return InetAddress.getLoopbackAddress();
        }
        try {
            return InetAddress.getByName(address.trim());
        } catch (UnknownHostException e) {
            return InetAddress.getLoopbackAddress();
        }
    }

    /**
     * Finds the Play application root, or returns {@code null} when the resolved
     * directory does not contain a Play application.
     */
    private static File findApplicationRoot() {
        File root = Play.initialized && Play.applicationPath != null
                ? Play.applicationPath
                : new File(System.getProperty("application.path", "."));
        return new File(root, "conf/application.conf").exists() ? root : null;
    }

    /**
     * Makes sure test classes can be compiled from source by Play's application
     * classloader. {@link Play#init(File, String)} only registers {@code app} and
     * {@code conf}; the test runner module usually adds {@code test}, but it is
     * not present when tests are started from an IDE.
     */
    private static void addTestSourcePath() {
        if (Play.applicationPath == null) {
            return;
        }
        VirtualFile test = VirtualFile.open(Play.applicationPath).child("test");
        if (!Play.javaPath.contains(test)) {
            Play.javaPath.add(test);
        }
    }

    private static String getPlayId() {
        String playId = System.getProperty("play.id", "test");
        if (!(playId.startsWith("test-") && playId.length() >= 6)) {
            playId = "test";
        }
        return playId;
    }
}
