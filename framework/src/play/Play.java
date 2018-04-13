package play;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import play.cache.Cache;
import play.classloading.ApplicationClasses;
import play.classloading.ApplicationClassloader;
import play.deps.DependenciesManager;
import play.exceptions.PlayException;
import play.exceptions.RestartNeededException;
import play.exceptions.UnexpectedException;
import play.libs.IO;
import play.mvc.Http;
import play.mvc.Router;
import play.plugins.PluginCollection;
import play.templates.TemplateLoader;
import play.utils.OrderSafeProperties;
import play.vfs.VirtualFile;

/**
 * Main framework class
 */
public class Play {

    /**
     * 2 modes
     */
    public enum Mode {

        /**
         * Enable development-specific features, e.g. view the documentation at the URL
         * {@literal "/@documentation"}.
         */
        DEV,
        /**
         * Disable development-specific features.
         */
        PROD;

        public boolean isDev() {
            return this == DEV;
        }

        public boolean isProd() {
            return this == PROD;
        }
    }

    /**
     * Is the application initialized
     */
    public static boolean initialized = false;

    /**
     * Is the application started
     */
    public static boolean started = false;
    /**
     * True when the one and only shutdown hook is enabled
     */
    private static boolean shutdownHookEnabled = false;
    /**
     * The framework ID
     */
    public static String id = System.getProperty("play.id", "");
    /**
     * The application mode
     */
    public static Mode mode = Mode.DEV;
    /**
     * The application root
     */
    public static File applicationPath = new File(System.getProperty("application.path", "."));
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
    public static List<VirtualFile> roots = new ArrayList<>(16);
    /**
     * All paths to search for Java files
     */
    public static List<VirtualFile> javaPath = new CopyOnWriteArrayList<>();
    /**
     * All paths to search for templates files
     */
    public static List<VirtualFile> templatesPath = new ArrayList<>(2);
    /**
     * Main routes file
     */
    public static VirtualFile routes;
    /**
     * Plugin routes files
     */
    public static Map<String, VirtualFile> modulesRoutes = new HashMap<>(16);
    /**
     * The loaded configuration files
     */
    public static Set<VirtualFile> confs = new HashSet<>(1);
    /**
     * The app configuration (already resolved from the framework id)
     */
    public static Properties configuration = new Properties();
    /**
     * The last time than the application has started
     */
    public static long startedAt;
    /**
     * The list of supported locales
     */
    public static List<String> langs = new ArrayList<>(16);
    /**
     * The very secret key
     */
    public static String secretKey;
    /**
     * pluginCollection that holds all loaded plugins and all enabled plugins..
     */
    public static PluginCollection pluginCollection = new PluginCollection();
    /**
     * Readonly list containing currently enabled plugins. This list is updated from
     * pluginCollection when pluginCollection is modified Play plugins Use
     * pluginCollection instead.
     */
    @Deprecated
    public static List<PlayPlugin> plugins = pluginCollection.getEnabledPlugins();
    /**
     * Modules
     */
    public static Map<String, VirtualFile> modules = new HashMap<>(16);
    /**
     * Framework version
     */
    public static String version = null;
    /**
     * Context path (when several application are deployed on the same host)
     */
    public static String ctxPath = "";
    static boolean firstStart = true;
    public static boolean usePrecompiled = false;
    public static boolean forceProd = false;
    /**
     * Lazy load the templates on demand
     */
    public static boolean lazyLoadTemplates = false;

    /**
     * This is used as default encoding everywhere related to the web: request,
     * response, WS
     */
    public static String defaultWebEncoding = "utf-8";

    /**
     * This flag indicates if the app is running in a standalone Play server or as a
     * WAR in an applicationServer
     */
    public static boolean standalonePlayServer = true;

    /**
     * Init the framework
     *
     * @param root
     *            The application path
     * @param id
     *            The framework id to use
     */
    public static void init(File root, String id) {
        // Simple things
        Play.id = id;
        Play.started = false;
        Play.applicationPath = root;

        // load all play.static of exists
        initStaticStuff();

        guessFrameworkPath();

        // Read the configuration file
        readConfiguration();

        Play.classes = new ApplicationClasses();

        // Configure logs
        Logger.init();
        String logLevel = configuration.getProperty("application.log", "INFO");

        // only override log-level if Logger was not configured manually
        if (!Logger.configuredManually) {
            Logger.setUp(logLevel);
        }
        Logger.recordCaller = Boolean.parseBoolean(configuration.getProperty("application.log.recordCaller", "false"));

        Logger.info("Starting %s", root.getAbsolutePath());

        if (configuration.getProperty("play.tmp", "tmp").equals("none")) {
            tmpDir = null;
            Logger.debug("No tmp folder will be used (play.tmp is set to none)");
        } else {
            tmpDir = new File(configuration.getProperty("play.tmp", "tmp"));
            if (!tmpDir.isAbsolute()) {
                tmpDir = new File(applicationPath, tmpDir.getPath());
            }

            if (Logger.isTraceEnabled()) {
                Logger.trace("Using %s as tmp dir", Play.tmpDir);
            }

            if (!tmpDir.exists()) {
                try {
                    if (readOnlyTmp) {
                        throw new Exception("ReadOnly tmp");
                    }
                    tmpDir.mkdirs();
                } catch (Throwable e) {
                    tmpDir = null;
                    Logger.warn("No tmp folder will be used (cannot create the tmp dir), caused by: %s", e);
                }
            }
        }

        // Mode
        try {
            mode = Mode.valueOf(configuration.getProperty("application.mode", "DEV").toUpperCase());
        } catch (IllegalArgumentException e) {
            Logger.error("Illegal mode '%s', use either prod or dev", configuration.getProperty("application.mode"));
            fatalServerErrorOccurred();
        }

        // Force the Production mode if forceProd or precompile is activate
        // Set to the Prod mode must be done before loadModules call
        // as some modules (e.g. DocViewer) is only available in DEV
        if (usePrecompiled || forceProd || System.getProperty("precompile") != null) {
            mode = Mode.PROD;
        }

        // Context path
        ctxPath = configuration.getProperty("http.path", ctxPath);

        // Build basic java source path
        VirtualFile appRoot = VirtualFile.open(applicationPath);
        roots.clear();
        roots.add(appRoot);

        javaPath.clear();
        javaPath.add(appRoot.child("app"));
        javaPath.add(appRoot.child("conf"));

        // Build basic templates path
        templatesPath.clear();
        if (appRoot.child("app/views").exists()
                || (usePrecompiled && appRoot.child("precompiled/templates/app/views").exists())) {
            templatesPath.add(appRoot.child("app/views"));
        }

        // Main route file
        routes = appRoot.child("conf/routes");

        // Plugin route files
        modulesRoutes.clear();

        // Load modules
        loadModules(appRoot);

        // Load the templates from the framework after the one from the modules
        templatesPath.add(VirtualFile.open(new File(frameworkPath, "framework/templates")));

        // Enable a first classloader
        classloader = new ApplicationClassloader();

        // Fix ctxPath
        if ("/".equals(Play.ctxPath)) {
            Play.ctxPath = "";
        }

        // Default cookie domain
        Http.Cookie.defaultDomain = configuration.getProperty("application.defaultCookieDomain", null);
        if (Http.Cookie.defaultDomain != null) {
            Logger.info("Using default cookie domain: " + Http.Cookie.defaultDomain);
        }

        // Plugins
        pluginCollection.loadPlugins();

        // Done !
        if (mode == Mode.PROD) {
            if (preCompile() && System.getProperty("precompile") == null) {
                start();
            } else {
                return;
            }
        } else {
            Logger.warn("You're running Play! in DEV mode");
        }

        // Plugins
        pluginCollection.onApplicationReady();

        Play.initialized = true;
    }

    public static void guessFrameworkPath() {
        // Guess the framework path
        try {
            URL versionUrl = Play.class.getResource("/play/version");
            // Read the content of the file
            Play.version = new LineNumberReader(new InputStreamReader(versionUrl.openStream())).readLine();

            // This is used only by the embedded server (Mina, Netty, Jetty etc)
            URI uri = new URI(versionUrl.toString().replace(" ", "%20"));
            if (frameworkPath == null || !frameworkPath.exists()) {
                if (uri.getScheme().equals("jar")) {
                    String jarPath = uri.getSchemeSpecificPart().substring(5,
                            uri.getSchemeSpecificPart().lastIndexOf("!"));
                    frameworkPath = new File(jarPath).getParentFile().getParentFile().getAbsoluteFile();
                } else if (uri.getScheme().equals("file")) {
                    frameworkPath = new File(uri).getParentFile().getParentFile().getParentFile().getParentFile();
                } else {
                    throw new UnexpectedException(
                            "Cannot find the Play! framework - trying with uri: " + uri + " scheme " + uri.getScheme());
                }
            }
        } catch (Exception e) {
            throw new UnexpectedException("Where is the framework ?", e);
        }
    }

    /**
     * Read application.conf and resolve overridden key using the play id mechanism.
     */
    public static void readConfiguration() {
        confs = new HashSet<>();
        configuration = readOneConfigurationFile("application.conf");
        extractHttpPort();
        // Plugins
        pluginCollection.onConfigurationRead();
    }

    private static void extractHttpPort() {
        String javaCommand = System.getProperty("sun.java.command", "");
        jregex.Matcher m = new jregex.Pattern(".* --http.port=({port}\\d+)").matcher(javaCommand);
        if (m.matches()) {
            configuration.setProperty("http.port", m.group("port"));
        }
    }

    private static Properties readOneConfigurationFile(String filename) {
        Properties propsFromFile = null;

        VirtualFile appRoot = VirtualFile.open(applicationPath);

        VirtualFile conf = appRoot.child("conf/" + filename);
        if (confs.contains(conf)) {
            throw new RuntimeException("Detected recursive @include usage. Have seen the file " + filename + " before");
        }

        try {
            propsFromFile = IO.readUtf8Properties(conf.inputstream());
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                Logger.fatal("Cannot read " + filename);
                fatalServerErrorOccurred();
            }
        }
        confs.add(conf);

        // OK, check for instance specifics configuration
        Properties newConfiguration = new OrderSafeProperties();
        Pattern pattern = Pattern.compile("^%([a-zA-Z0-9_\\-]+)\\.(.*)$");
        for (Object key : propsFromFile.keySet()) {
            Matcher matcher = pattern.matcher(key + "");
            if (!matcher.matches()) {
                newConfiguration.put(key, propsFromFile.get(key).toString().trim());
            }
        }
        for (Object key : propsFromFile.keySet()) {
            Matcher matcher = pattern.matcher(key + "");
            if (matcher.matches()) {
                String instance = matcher.group(1);
                if (instance.equals(id)) {
                    newConfiguration.put(matcher.group(2), propsFromFile.get(key).toString().trim());
                }
            }
        }
        propsFromFile = newConfiguration;
        // Resolve ${..}
        pattern = Pattern.compile("\\$\\{([^}]+)}");
        for (Object key : propsFromFile.keySet()) {
            String value = propsFromFile.getProperty(key.toString());
            Matcher matcher = pattern.matcher(value);
            StringBuffer newValue = new StringBuffer(100);
            while (matcher.find()) {
                String jp = matcher.group(1);
                String r;
                if (jp.equals("application.path")) {
                    r = Play.applicationPath.getAbsolutePath();
                } else if (jp.equals("play.path")) {
                    r = Play.frameworkPath.getAbsolutePath();
                } else {
                    r = System.getProperty(jp);
                    if (r == null) {
                        r = System.getenv(jp);
                    }
                    if (r == null) {
                        Logger.warn("Cannot replace %s in configuration (%s=%s)", jp, key, value);
                        continue;
                    }
                }
                matcher.appendReplacement(newValue, r.replaceAll("\\\\", "\\\\\\\\"));
            }
            matcher.appendTail(newValue);
            propsFromFile.setProperty(key.toString(), newValue.toString());
        }
        // Include
        Map<Object, Object> toInclude = new HashMap<>(16);
        for (Object key : propsFromFile.keySet()) {
            if (key.toString().startsWith("@include.")) {
                try {
                    String filenameToInclude = propsFromFile.getProperty(key.toString());
                    toInclude.putAll(readOneConfigurationFile(filenameToInclude));
                } catch (Exception ex) {
                    Logger.warn(ex, "Missing include: %s", key);
                }
            }
        }
        propsFromFile.putAll(toInclude);

        return propsFromFile;
    }

    /**
     * Start the application. Recall to restart !
     */
    public static synchronized void start() {
        try {

            if (started) {
                stop();
            }

            if (standalonePlayServer) {
                // Can only register shutdown-hook if running as standalone server
                if (!shutdownHookEnabled) {
                    // registers shutdown hook - Now there's a good chance that we can notify
                    // our plugins that we're going down when some calls ctrl+c or just kills our
                    // process..
                    shutdownHookEnabled = true;
                    Thread hook = new Thread() {
                        @Override
                        public void run() {
                            Play.stop();
                        }
                    };
                    hook.setContextClassLoader(ClassLoader.getSystemClassLoader());
                    Runtime.getRuntime().addShutdownHook(hook);
                }
            }

            if (mode == Mode.DEV) {
                // Need a new classloader
                classloader = new ApplicationClassloader();
                // Put it in the current context for any code that relies on having it there
                Thread.currentThread().setContextClassLoader(classloader);
                // Reload plugins
                pluginCollection.reloadApplicationPlugins();

            }

            // Reload configuration
            readConfiguration();

            // Configure logs
            String logLevel = configuration.getProperty("application.log", "INFO");
            // only override log-level if Logger was not configured manually
            if (!Logger.configuredManually) {
                Logger.setUp(logLevel);
            }
            Logger.recordCaller = Boolean
                    .parseBoolean(configuration.getProperty("application.log.recordCaller", "false"));

            // Locales
            langs = new ArrayList<>(Arrays.asList(configuration.getProperty("application.langs", "").split(",")));
            if (langs.size() == 1 && langs.get(0).trim().length() == 0) {
                langs = new ArrayList<>(16);
            }

            // Clean templates
            TemplateLoader.cleanCompiledCache();

            // SecretKey
            secretKey = configuration.getProperty("application.secret", "").trim();
            if (secretKey.length() == 0) {
                Logger.warn("No secret key defined. Sessions will not be encrypted");
            }

            // Default web encoding
            String _defaultWebEncoding = configuration.getProperty("application.web_encoding");
            if (_defaultWebEncoding != null) {
                Logger.info("Using custom default web encoding: " + _defaultWebEncoding);
                defaultWebEncoding = _defaultWebEncoding;
                // Must update current response also, since the request/response triggering
                // this configuration-loading in dev-mode have already been
                // set up with the previous encoding
                if (Http.Response.current() != null) {
                    Http.Response.current().encoding = _defaultWebEncoding;
                }
            }

            // Try to load all classes
            Play.classloader.getAllClasses();

            // Routes
            Router.detectChanges(ctxPath);

            // Cache
            Cache.init();

            // Plugins
            try {
                pluginCollection.onApplicationStart();
            } catch (Exception e) {
                if (Play.mode.isProd()) {
                    Logger.error(e, "Can't start in PROD mode with errors");
                }
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new UnexpectedException(e);
            }

            if (firstStart) {
                Logger.info("Application '%s' is now started !", configuration.getProperty("application.name", ""));
                firstStart = false;
            }

            // We made it
            started = true;
            startedAt = System.currentTimeMillis();

            // Plugins
            pluginCollection.afterApplicationStart();

        } catch (PlayException e) {
            started = false;
            try {
                Cache.stop();
            } catch (Exception ignored) {
            }
            throw e;
        } catch (Exception e) {
            started = false;
            try {
                Cache.stop();
            } catch (Exception ignored) {
            }
            throw new UnexpectedException(e);
        }
    }

    /**
     * Stop the application
     */
    public static synchronized void stop() {
        if (started) {
            Logger.trace("Stopping the play application");
            pluginCollection.onApplicationStop();
            started = false;
            Cache.stop();
            Router.lastLoading = 0L;
            Invoker.resetClassloaders();
        }
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
            fatalServerErrorOccurred();
            return false;
        }
        try {
            Logger.info("Precompiling ...");
            Thread.currentThread().setContextClassLoader(Play.classloader);
            long start = System.currentTimeMillis();
            classloader.getAllClasses();

            if (Logger.isTraceEnabled()) {
                Logger.trace("%sms to precompile the Java stuff", System.currentTimeMillis() - start);
            }

            if (!lazyLoadTemplates) {
                start = System.currentTimeMillis();
                TemplateLoader.getAllTemplate();

                if (Logger.isTraceEnabled()) {
                    Logger.trace("%sms to precompile the templates", System.currentTimeMillis() - start);
                }
            }
            return true;
        } catch (Throwable e) {
            Logger.error(e, "Cannot start in PROD mode with errors");
            fatalServerErrorOccurred();
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
            pluginCollection.beforeDetectingChanges();
            if (!pluginCollection.detectClassesChange()) {
                classloader.detectChanges();
            }
            Router.detectChanges(ctxPath);
            pluginCollection.detectChange();
            if (!Play.started) {
                throw new RestartNeededException("Not started");
            }
        } catch (PlayException e) {
            throw e;
        } catch (RestartNeededException e) {
            if (started) {
                if (e.getCause() != null && e.getCause() != e) {
                    Logger.info("Restart: " + e.getMessage() + ", caused by: " + e.getCause());
                } else {
                    Logger.info("Restart: " + e.getMessage());
                }
            }
            start();
        } catch (Exception e) {
            Logger.error(e, "Restart: " + e.getMessage());
            start();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends PlayPlugin> T plugin(Class<T> clazz) {
        return pluginCollection.getPluginInstance(clazz);
    }

    /**
     * Allow some code to run very early in Play - Use with caution !
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
                        Logger.warn(e, "! Cannot init static: " + line);
                    }
                }
            } catch (Exception ex) {
                Logger.error(ex, "Cannot load %s", url);
            }
        }
    }

    /**
     * Load all modules. You can even specify the list using the MODULES environment
     * variable.
     */
    public static void loadModules() {
        loadModules(VirtualFile.open(applicationPath));
    }

    /**
     * Load all modules. You can even specify the list using the MODULES environment
     * variable.
     * 
     * @param appRoot
     *            the application path virtual file
     */
    public static void loadModules(VirtualFile appRoot) {
        if (System.getenv("MODULES") != null) {
            // Modules path is prepended with a env property
            if (System.getenv("MODULES") != null && System.getenv("MODULES").trim().length() > 0) {

                for (String m : System.getenv("MODULES").split(File.pathSeparator)) {
                    File modulePath = new File(m);
                    if (!modulePath.exists() || !modulePath.isDirectory()) {
                        Logger.error("Module %s will not be loaded because %s does not exist", modulePath.getName(),
                                modulePath.getAbsolutePath());
                    } else {
                        String modulePathName = modulePath.getName();
                        String moduleName = modulePathName.contains("-")
                                ? modulePathName.substring(0, modulePathName.lastIndexOf("-"))
                                : modulePathName;
                        addModule(appRoot, moduleName, modulePath);
                    }
                }
            }
        }

        // Load modules from modules/ directory, but get the order from the
        // dependencies.yml file
        // .listFiles() returns items in an OS dependant sequence, which is bad
        // See #781
        // the yaml parser wants play.version as an environment variable
        System.setProperty("play.version", Play.version);
        System.setProperty("application.path", applicationPath.getAbsolutePath());

        File localModules = Play.getFile("modules");
        Set<String> modules = new LinkedHashSet<>();
        if (localModules != null && localModules.exists() && localModules.isDirectory()) {
            try {
                File userHome = new File(System.getProperty("user.home"));
                DependenciesManager dm = new DependenciesManager(applicationPath, frameworkPath, userHome);
                modules = dm.retrieveModules();
            } catch (Exception e) {
                Logger.error(
                        "There was a problem parsing dependencies.yml (module will not be loaded in order of the dependencies.yml)",
                        e);
                // Load module without considering the dependencies.yml order
                modules.addAll(Arrays.asList(localModules.list()));
            }

            for (Iterator<String> iter = modules.iterator(); iter.hasNext();) {
                String moduleName = iter.next();

                File module = new File(localModules, moduleName);

                if (moduleName.contains("-")) {
                    moduleName = moduleName.substring(0, moduleName.indexOf("-"));
                }

                if (module == null || !module.exists()) {
                    Logger.error("Module %s will not be loaded because %s does not exist", moduleName,
                            module.getAbsolutePath());
                } else if (module.isDirectory()) {
                    addModule(appRoot, moduleName, module);
                } else {
                    File modulePath = new File(IO.readContentAsString(module).trim());
                    if (!modulePath.exists() || !modulePath.isDirectory()) {
                        Logger.error("Module %s will not be loaded because %s does not exist", moduleName,
                                modulePath.getAbsolutePath());
                    } else {
                        addModule(appRoot, moduleName, modulePath);
                    }
                }
            }
        }

        // Auto add special modules
        if (Play.runningInTestMode()) {
            addModule(appRoot, "_testrunner", new File(Play.frameworkPath, "modules/testrunner"));
        }
    }

    /**
     * Add a play application (as plugin)
     *
     * @param name
     *            the module name
     * @param path
     *            The application path
     */
    public static void addModule(String name, File path) {
        addModule(VirtualFile.open(applicationPath), name, path);
    }

    /**
     * Add a play application (as plugin)
     *
     * @param appRoot
     *            the application path virtual file
     * @param name
     *            the module name
     * @param path
     *            The application path
     */
    public static void addModule(VirtualFile appRoot, String name, File path) {
        VirtualFile root = VirtualFile.open(path);
        modules.put(name, root);
        if (root.child("app").exists()) {
            javaPath.add(root.child("app"));
        }
        if (root.child("app/views").exists() || (usePrecompiled
                && appRoot.child("precompiled/templates/from_module_" + name + "/app/views").exists())) {
            templatesPath.add(root.child("app/views"));
        }
        if (root.child("conf/routes").exists() || (usePrecompiled
                && appRoot.child("precompiled/templates/from_module_" + name + "/conf/routes").exists())) {
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
     * @param path
     *            Relative path from the applications root
     * @return The virtualFile or null
     */
    public static VirtualFile getVirtualFile(String path) {
        return VirtualFile.search(roots, path);
    }

    /**
     * Search a File in the current application
     *
     * @param path
     *            Relative path from the application root
     * @return The file even if it doesn't exist
     */
    public static File getFile(String path) {
        return new File(applicationPath, path);
    }

    /**
     * Returns true if application is running in test-mode. Test-mode is resolved
     * from the framework id.
     *
     * Your app is running in test-mode if the framework id (Play.id) is 'test' or
     * 'test-?.*'
     * 
     * @return true if test mode
     */
    public static boolean runningInTestMode() {
        return id.matches("test|test-?.*");
    }

    /**
     * Call this method when there has been a fatal error that Play cannot recover
     * from
     */
    public static void fatalServerErrorOccurred() {
        if (standalonePlayServer) {
            // Just quit the process
            System.exit(-1);
        } else {
            // Cannot quit the process while running inside an applicationServer
            String msg = "A fatal server error occurred";
            Logger.error(msg);
            throw new Error(msg);
        }
    }

    public static boolean useDefaultMockMailSystem() {
        return configuration.getProperty("mail.smtp", "").equals("mock") && mode == Mode.DEV;
    }

}
