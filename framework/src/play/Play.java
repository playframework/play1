package play;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import play.cache.Cache;
import play.classloading.ApplicationClasses;
import play.classloading.ApplicationClassloader;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.libs.IO;
import play.mvc.Router;
import play.templates.TemplateLoader;
import play.vfs.VirtualFile;

/**
 * Main framework class
 */
public class Play {

    /**
     * 2 modes
     */
    public enum Mode {

        DEV, PROD
    }

    /**
     * Is the application started
     */
    public static boolean started = false;
    /**
     * The framework ID
     */
    public static String id;
    /**
     * The application mode
     */
    public static Mode mode;
    /**
     * The application root
     */
    public static File applicationPath = null;
    /**
     * tmp dir
     */
    public static File tmpDir = null;
    /**
     * tmp dir is readOnly
     */
    public static boolean readOnlyTmp = false;
    /**
     * The framework root
     */
    public static File frameworkPath = null;
    /**
     * All loaded application classes
     */
    public static ApplicationClasses classes;
    /**
     * The application classLoader
     */
    public static ApplicationClassloader classloader;
    /**
     * All paths to search for files
     */
    public static List<VirtualFile> roots = new ArrayList<VirtualFile>();
    /**
     * All paths to search for Java files
     */
    public static List<VirtualFile> javaPath;
    /**
     * All paths to search for templates files
     */
    public static List<VirtualFile> templatesPath;
    /**
     * Main routes file
     */
    public static VirtualFile routes;
    /**
     * Plugin routes files
     */
    public static Map<String, VirtualFile> modulesRoutes;
    /**
     * The main application.conf
     */
    public static VirtualFile conf;
    /**
     * The app configuration (already resolved from the framework id)
     */
    public static Properties configuration;
    /**
     * The last time than the application has started
     */
    public static long startedAt;
    /**
     * The list of supported locales
     */
    public static List<String> langs = new ArrayList<String>();
    /**
     * The very secret key
     */
    public static String secretKey;
    /**
     * Play plugins
     */
    public static List<PlayPlugin> plugins = new ArrayList<PlayPlugin>();
    /**
     * Modules
     */
    public static Map<String, VirtualFile> modules = new HashMap<String, VirtualFile>();
    /**
     * Framework version
     */
    public static String version = null;
    // pv
    static boolean firstStart = true;
    public static boolean usePrecompiled = false;
    /**
     * Lazy load the templates on demand
     */
    public static boolean lazyLoadTemplates = false;
    private final static String NO_PREFIX = "";

    /**
     * Init the framework
     *
     * @param root The application path
     * @param id   The framework id to use
     */
    public static void init(File root, String id) {
        // Simple things
        Play.id = id;
        Play.started = false;
        Play.applicationPath = root;

        // load all play.static of exists
        initStaticStuff();

        // Guess the framework path
        try {

            URL versionUrl = Play.class.getResource("/play/version");
            // Read the content of the file
            Play.version = new LineNumberReader(new InputStreamReader(versionUrl.openStream())).readLine();

            // This is used only by the embedded server (Mina, Netty, Jetty etc)
            URI uri = new URI(versionUrl.toString().replace(" ", "%20"));
            if (frameworkPath == null || !frameworkPath.exists()) {
                if (uri.getScheme().equals("jar")) {
                    String jarPath = uri.getSchemeSpecificPart().substring(5, uri.getSchemeSpecificPart().lastIndexOf("!"));
                    frameworkPath = new File(jarPath).getParentFile().getParentFile().getAbsoluteFile();
                } else if (uri.getScheme().equals("file")) {
                    frameworkPath = new File(uri).getParentFile().getParentFile().getParentFile().getParentFile();
                } else {
                    throw new UnexpectedException("Cannot find the Play! framework - trying with uri: " + uri + " scheme " + uri.getScheme());
                }
            }
        } catch (Exception e) {
            throw new UnexpectedException("Where is the framework ?", e);
        }

        System.setProperty("play.path", frameworkPath.getAbsolutePath());
        System.setProperty("application.path", applicationPath.getAbsolutePath());

        // Read the configuration file
        readConfiguration();

        Play.classes = new ApplicationClasses();

        // Configure logs
        Logger.init();
        String logLevel = configuration.getProperty("application.log", "INFO");
        Logger.setUp(logLevel);

        Logger.info("Starting %s", root.getAbsolutePath());

        if (configuration.getProperty("play.tmp", "tmp").equals("none")) {
            tmpDir = null;
            Logger.debug("No tmp folder will be used (play.tmp is set to none)");
        } else {
            tmpDir = new File(configuration.getProperty("play.tmp", "tmp"));
            if (!tmpDir.isAbsolute()) {
                tmpDir = new File(applicationPath, tmpDir.getPath());
            }
            Logger.trace("Using %s as tmp dir", Play.tmpDir);
            if (!tmpDir.exists()) {
                try {
                    if (readOnlyTmp) {
                        throw new Exception("ReadOnly tmp");
                    }
                    tmpDir.mkdirs();
                } catch (Throwable e) {
                    tmpDir = null;
                    Logger.warn("No tmp folder will be used (cannot create the tmp dir)");
                }
            }
        }

        // Mode
        mode = Mode.valueOf(configuration.getProperty("application.mode", "DEV").toUpperCase());

        // Build basic java source path
        VirtualFile appRoot = VirtualFile.open(applicationPath);
        roots.add(appRoot);
        javaPath = new ArrayList<VirtualFile>();
        javaPath.add(appRoot.child("app"));
        javaPath.add(appRoot.child("conf"));

        // Build basic templates path
        templatesPath = new ArrayList<VirtualFile>();
        templatesPath.add(appRoot.child("app/views"));
        templatesPath.add(VirtualFile.open(new File(frameworkPath, "framework/templates")));

        // Main route file
        routes = appRoot.child("conf/routes");

        // Plugin route files
        modulesRoutes = new HashMap<String, VirtualFile>();

        // Load modules
        loadModules();

        // Enable a first classloader
        classloader = new ApplicationClassloader();

        // Plugins
        loadPlugins();

        // Done !
        if (mode == Mode.PROD || System.getProperty("precompile") != null) {
            mode = Mode.PROD;
            if (preCompile() && System.getProperty("precompile") == null) {
                start();
            } else {
                return;
            }
        } else {
            Logger.warn("You're running Play! in DEV mode");
        }
    }

    /**
     * Read application.conf and resolve overriden key using the play id mechanism.
     */
    @SuppressWarnings("unchecked")
    static void readConfiguration() {
        VirtualFile appRoot = VirtualFile.open(applicationPath);
        conf = appRoot.child("conf/application.conf");
        try {
            configuration = IO.readUtf8Properties(conf.inputstream());
        } catch (IOException ex) {
            Logger.fatal("Cannot read application.conf");
            System.exit(0);
        }
        // Ok, check for instance specifics configuration
        Properties newConfiguration = new Properties();
        Pattern pattern = Pattern.compile("^%([a-zA-Z0-9_\\-]+)\\.(.*)$");
        for (Object key : configuration.keySet()) {
            Matcher matcher = pattern.matcher(key + "");
            if (!matcher.matches()) {
                newConfiguration.put(key, configuration.get(key).toString().trim());
            }
        }
        for (Object key : configuration.keySet()) {
            Matcher matcher = pattern.matcher(key + "");
            if (matcher.matches()) {
                String instance = matcher.group(1);
                if (instance.equals(id)) {
                    newConfiguration.put(matcher.group(2), configuration.get(key).toString().trim());
                }
            }
        }
        configuration = newConfiguration;
        // Resolve ${..}
        pattern = Pattern.compile("\\$\\{([^}]+)}");
        for (Object key : configuration.keySet()) {
            String value = configuration.getProperty(key.toString());
            Matcher matcher = pattern.matcher(value);
            StringBuffer newValue = new StringBuffer();
            while (matcher.find()) {
                String jp = matcher.group(1);
                String r = System.getProperty(jp);
                if (r == null) {
                    Logger.warn("Cannot replace %s in configuration (%s=%s)", jp, key, value);
                    continue;
                }
                matcher.appendReplacement(newValue, System.getProperty(jp).replaceAll("\\\\", "\\\\\\\\"));
            }
            matcher.appendTail(newValue);
            configuration.setProperty(key.toString(), newValue.toString());
        }
        // Include
        Map toInclude = new HashMap();
        for (Object key : configuration.keySet()) {
            if (key.toString().startsWith("@include.")) {
                try {
                    toInclude.putAll(IO.readUtf8Properties(appRoot.child("conf/" + configuration.getProperty(key.toString())).inputstream()));
                } catch (Exception ex) {
                    Logger.warn("Missing include: %s", key);
                }
            }
        }
        configuration.putAll(toInclude);
        // Plugins
        for (PlayPlugin plugin : plugins) {
            plugin.onConfigurationRead();
        }
    }

    /**
     * Start the application.
     * Recall to restart !
     */
    public static synchronized void start() {
        try {

            if (started) {
                stop();
            }

            if (mode == Mode.DEV) {
                // Need a new classloader
                classloader = new ApplicationClassloader();
                // Reload plugins
                List<PlayPlugin> newPlugins = new ArrayList<PlayPlugin>();
                for (PlayPlugin plugin : plugins) {
                    if (plugin.getClass().getClassLoader().getClass().equals(ApplicationClassloader.class)) {
                        PlayPlugin newPlugin = (PlayPlugin) classloader.loadClass(plugin.getClass().getName()).getConstructors()[0].newInstance();
                        newPlugin.onLoad();
                        newPlugins.add(newPlugin);
                    } else {
                        newPlugins.add(plugin);
                    }
                }
                plugins = newPlugins;
            }

            // Reload configuration
            readConfiguration();

            // Configure logs
            String logLevel = configuration.getProperty("application.log", "INFO");
            Logger.setUp(logLevel);

            // Locales
            langs = Arrays.asList(configuration.getProperty("application.langs", "").split(","));
            if (langs.size() == 1 && langs.get(0).trim().equals("")) {
                langs = new ArrayList<String>();
            }

            // Clean templates
            TemplateLoader.cleanCompiledCache();

            // SecretKey
            secretKey = configuration.getProperty("application.secret", "").trim();
            if (secretKey.equals("")) {
                Logger.warn("No secret key defined. Sessions will not be encrypted");
            }

            // Try to load all classes
            Play.classloader.getAllClasses();

            // Routes
            Router.detectChanges(NO_PREFIX);

            // Plugins
            for (PlayPlugin plugin : plugins) {
                plugin.onApplicationStart();
            }

            // Cache
            Cache.init();

            if (firstStart) {
                Logger.info("Application '%s' is now started !", configuration.getProperty("application.name", ""));
                firstStart = false;
            }

            // We made it
            started = true;
            startedAt = System.currentTimeMillis();

            // Plugins
            for (PlayPlugin plugin : plugins) {
                plugin.afterApplicationStart();
            }

        } catch (PlayException e) {
            started = false;
            throw e;
        } catch (Exception e) {
            started = false;
            throw new UnexpectedException(e);
        }
    }

    /**
     * Stop the application
     */
    public static synchronized void stop() {
        started = false;
        for (PlayPlugin plugin : plugins) {
            plugin.onApplicationStop();
        }
        Cache.stop();
        Router.lastLoading = 0L;
    }

    /**
     * Force all java source and template compilation.
     *
     * @return success ?
     */
    static boolean preCompile() {
        if (usePrecompiled) {
            if (Play.getFile("precompiled").exists()) {
                classloader.getAllClasses();
                Logger.info("Application is precompiled");
                return true;
            }
            Logger.error("Precompiled classes are missing!!");
            try {
                System.exit(-1);
            } catch (Exception ex) {
                // Will not work in some application servers
            }
            return false;
        }
        try {
            Logger.info("Precompiling ...");
            long start = System.currentTimeMillis();
            classloader.getAllClasses();
            Logger.trace("%sms to precompile the Java stuff", System.currentTimeMillis() - start);
            if (!lazyLoadTemplates) {
                start = System.currentTimeMillis();
                TemplateLoader.getAllTemplate();
                Logger.trace("%sms to precompile the templates", System.currentTimeMillis() - start);
            }
            return true;
        } catch (Throwable e) {
            Logger.error(e, "Cannot start in PROD mode with errors");
            try {
                System.exit(-1);
            } catch (Exception ex) {
                // Will not work in some application servers
            }
            return false;
        }
    }

    /**
     * Detect sources modifications
     */
    public static synchronized void detectChanges() {
        if (mode == Mode.PROD) {
            return;
        }
        try {
            for (PlayPlugin plugin : plugins) {
                plugin.beforeDetectingChanges();
            }
            classloader.detectChanges();
            Router.detectChanges(NO_PREFIX);
            if (conf.lastModified() > startedAt) {
                start();
                return;
            }
            for (PlayPlugin plugin : plugins) {
                plugin.detectChange();
            }
            if (!Play.started) {
                throw new RuntimeException("Not started");
            }
        } catch (PlayException e) {
            throw e;
        } catch (Exception e) {
            // We have to do a clean refresh
            start();
        }
    }

    public static <T> T plugin(Class<T> clazz) {
        for(PlayPlugin p : plugins) {
            if(clazz.isInstance(p)) {
                return (T)p;
            }
        }
        return null;
    }

    /**
     * Enable found plugins
     */
    public static void loadPlugins() {
        Logger.trace("Loading plugins");
        // Play! plugings
        Enumeration<URL> urls = null;
        try {
            urls = Play.classloader.getResources("play.plugins");
        } catch (Exception e) {
        }
        while (urls != null && urls.hasMoreElements()) {
            URL url = urls.nextElement();
            Logger.trace("Found one plugins descriptor, %s", url);
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    String[] infos = line.split(":");
                    PlayPlugin plugin = (PlayPlugin) Play.classloader.loadClass(infos[1]).newInstance();
                    Logger.trace("Loaded plugin %s", plugin);
                    plugin.index = Integer.parseInt(infos[0]);
                    plugins.add(plugin);
                }
            } catch (Exception ex) {
                Logger.error(ex, "Cannot load %s", url);
            }
        }
        Collections.sort(plugins);
        for (PlayPlugin plugin : new ArrayList<PlayPlugin>(plugins)) { // wrap a new collection to allow some plugins to modify the list
            plugin.onLoad();
        }
    }

    /**
     * Allow some code to run very eraly in Play! - Use with caution !
     */
    public static void initStaticStuff() {
        // Play! plugings
        Enumeration<URL> urls = null;
        try {
            urls = Play.class.getClassLoader().getResources("play.static");
        } catch (Exception e) {
        }
        while (urls != null && urls.hasMoreElements()) {
            URL url = urls.nextElement();
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    try {
                        Class.forName(line);
                    } catch (Exception e) {
                        System.out.println("! Cannot init static : " + line);
                    }
                }
            } catch (Exception ex) {
                Logger.error(ex, "Cannot load %s", url);
            }
        }
    }

    /**
     * Load all modules.
     * You can even specify the list using the MODULES environement variable.
     */
    public static void loadModules() {
        if (System.getenv("MODULES") != null) {
            // Modules path is prepended with a env property
            if (System.getenv("MODULES") != null && System.getenv("MODULES").trim().length() > 0) {
                for (String m : System.getenv("MODULES").split(System.getProperty("os.name").startsWith("Windows") ? ";" : ":")) {
                    File modulePath = new File(m);
                    if (!modulePath.exists() || !modulePath.isDirectory()) {
                        Logger.error("Module %s will not be loaded because %s does not exist", modulePath.getName(), modulePath.getAbsolutePath());
                    } else {
                        addModule(modulePath.getName(), modulePath);
                    }
                }
            }
        }
        for (Enumeration<?> e = configuration.propertyNames(); e.hasMoreElements();) {
            String pName = e.nextElement().toString();
            if (pName.startsWith("module.")) {
                String moduleName = pName.substring(7);
                File modulePath = new File(configuration.getProperty(pName));
                if (!modulePath.isAbsolute()) {
                    modulePath = new File(applicationPath, configuration.getProperty(pName));
                }
                if (!modulePath.exists() || !modulePath.isDirectory()) {
                    Logger.error("Module %s will not be loaded because %s does not exist", moduleName, modulePath.getAbsolutePath());
                } else {
                    addModule(moduleName, modulePath);
                }
            }
        }
        if (Play.id.equals("test")) {
            addModule("_testrunner", new File(Play.frameworkPath, "modules/testrunner"));
        }
        if (Play.mode == Mode.DEV) {
            addModule("_docviewer", new File(Play.frameworkPath, "modules/docviewer"));
        }
    }

    /**
     * Add a play application (as plugin)
     *
     * @param path The application path
     */
    public static void addModule(String name, File path) {
        VirtualFile root = VirtualFile.open(path);
        modules.put(name, root);
        if (root.child("app").exists()) {
            javaPath.add(root.child("app"));
        }
        if (root.child("app/views").exists()) {
            templatesPath.add(root.child("app/views"));
        }
        if (root.child("conf/routes").exists()) {
            modulesRoutes.put(name, root.child("conf/routes"));
        }
        roots.add(root);
        if (!name.startsWith("_")) {
            Logger.info("Module %s is available (%s)", name, path.getAbsolutePath());
        }
    }

    /**
     * Search a VirtualFile in all loaded applications and plugins
     *
     * @param path Relative path from the applications root
     * @return The virtualFile or null
     */
    public static VirtualFile getVirtualFile(String path) {
        return VirtualFile.search(roots, path);
    }

    /**
     * Search a File in the current application
     *
     * @param path Relative path from the application root
     * @return The file even if it doesn't exist
     */
    public static File getFile(String path) {
        return new File(applicationPath, path);
    }
}
