package play;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
import org.apache.log4j.Level;
import play.cache.Cache;
import play.classloading.ApplicationClasses;
import play.classloading.ApplicationClassloader;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.libs.IO;
import play.vfs.VirtualFile;
import play.mvc.Router;
import play.templates.TemplateLoader;

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
     * The framework root
     */
    public static File frameworkPath = null;
    /**
     * All loaded application classes
     */
    public static ApplicationClasses classes = new ApplicationClasses();
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
    public static List<VirtualFile> modules = new ArrayList<VirtualFile>();

    /**
     * Init the framework
     * @param root The application path
     * @param id The framework id to use
     */
    public static void init(File root, String id) {
        // Simple things
        Play.id = id;
        Play.started = false;
        Play.applicationPath = root;
        
        // Guess the framework path
        try {
            URI uri = Play.class.getResource("/play/version").toURI();
            if (uri.getScheme().equals("jar")) {
                String jarPath = uri.getSchemeSpecificPart().substring(5, uri.getSchemeSpecificPart().lastIndexOf("!"));
                frameworkPath = new File(jarPath).getParentFile().getParentFile().getAbsoluteFile();
            } else if (uri.getScheme().equals("file")) {
                frameworkPath = new File(uri).getParentFile().getParentFile().getParentFile().getParentFile();
            }
        } catch (Exception e) {
            throw new UnexpectedException("Where is the framework ?", e);
        }
        System.setProperty("play.path", Play.frameworkPath.getAbsolutePath());
        Logger.info("Starting %s", root.getAbsolutePath());
        
        // Read the configuration file
        readConfiguration();

        // Mode
        mode = Mode.valueOf(configuration.getProperty("application.mode", "DEV").toUpperCase());

        // Configure logs
        String logLevel = configuration.getProperty("application.log", "INFO");
        Logger.log4j.setLevel(Level.toLevel(logLevel));
        
        // Build basic java source path
        VirtualFile appRoot = VirtualFile.open(applicationPath);
        roots.add(appRoot);
        javaPath = new ArrayList<VirtualFile>();
        javaPath.add(appRoot.child("app"));
        if (id.equals("test")) {
            javaPath.add(appRoot.child("test"));
        }

        // Build basic templates path
        templatesPath = new ArrayList<VirtualFile>();
        templatesPath.add(appRoot.child("app/views"));
        templatesPath.add(VirtualFile.open(new File(frameworkPath, "framework/templates")));

        // Main route file
        routes = appRoot.child("conf/routes");

        // Plugin route files
        modulesRoutes = new HashMap<String, VirtualFile>();

        // Enable a first classloader
        classloader = new ApplicationClassloader();
        // tmp dir
        tmpDir = new File(configuration.getProperty("play.tmp", "tmp"));
        if (!tmpDir.isAbsolute()) {
            tmpDir = new File(applicationPath, tmpDir.getPath());
        }
        tmpDir.mkdirs();
        // Plugins
        bootstrapExtensions();

        if (mode == Mode.PROD) {
            preCompile();
            start();
        } else {
            Logger.warn("You're running Play! in DEV mode");
        }
        // Yop
        Logger.info("Application '%s' is ready !", configuration.getProperty("application.name", ""));
    }

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
                Logger.info("Reloading ...");
                stop();
            }
            if (mode == Mode.DEV) {
                // Need a new classloader
                classloader = new ApplicationClassloader();
                Thread.currentThread().setContextClassLoader(Play.classloader);
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
            // tmp dir
            tmpDir = new File(configuration.getProperty("play.tmp", "tmp"));
            if (!tmpDir.isAbsolute()) {
                tmpDir = new File(applicationPath, tmpDir.getPath());
            }
            tmpDir.mkdirs();
            // Configure logs
            String logLevel = configuration.getProperty("application.log", "INFO");
            Logger.log4j.setLevel(Level.toLevel(logLevel));
            // Locales
            langs = Arrays.asList(configuration.getProperty("application.langs", "").split(","));
            if (langs.size() == 1 && langs.get(0).trim().equals("")) {
                langs = new ArrayList<String>();
            }
            // Cache
            Cache.init();
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
            Router.detectChanges();

            // Plugins
            for (PlayPlugin plugin : plugins) {
                plugin.onApplicationStart();
            }

            // We made it
            started = true;
            startedAt = System.currentTimeMillis();

            // Plugins
            for (PlayPlugin plugin : plugins) {
                plugin.afterApplicationStart();
            }

        } catch (PlayException e) {
            throw e;
        } catch (Exception e) {
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

    static void preCompile() {
        try {
            ClassLoader l = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classloader);
            Logger.info("Precompiling ...");
            long start = System.currentTimeMillis();
            classloader.getAllClasses();
            Logger.trace("%sms to precompile the Java stuff", System.currentTimeMillis() - start);
            start = System.currentTimeMillis();
            TemplateLoader.getAllTemplate();
            Logger.trace("%sms to precompile the templates", System.currentTimeMillis() - start);
            Thread.currentThread().setContextClassLoader(l);
        } catch (Throwable e) {
            Logger.error(e, "Cannot start in PROD mode with errors");
            System.exit(-1);
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
            classloader.detectChanges();
            Router.detectChanges();
            if (conf.lastModified() > startedAt) {
                start();
                return;
            }
            for (PlayPlugin plugin : plugins) {
                plugin.detectChange();
            }
        } catch (PlayException e) {
            throw e;
        } catch (Exception e) {
            // We have to do a clean refresh
            start();
        }
    }

    /**
     * Enable found plugins
     */
    public static void bootstrapExtensions() {
        
        // Play! plugings
        Enumeration<URL> urls = null;
        try {
            urls = Play.class.getClassLoader().getResources("play.plugins");
        } catch (Exception e) {
        }
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    String[] infos = line.split(":");
                    PlayPlugin plugin = (PlayPlugin) Play.classloader.loadClass(infos[1]).newInstance();
                    plugin.index = Integer.parseInt(infos[0]);
                    plugins.add(plugin);
                }
            } catch (Exception ex) {
                Logger.error(ex, "Cannot load %s", url);
            }
        }
        Collections.sort(plugins);
        for (PlayPlugin plugin : plugins) {
            plugin.onLoad();
        }
        
        // Load modules
        if (System.getenv("MODULES") != null) { 
            // Modules path is overriden with a env property
            for(String m : System.getenv("MODULES").split(System.getProperty("os.name").startsWith("Windows") ? ";" : ":")) {
                File modulePath = new File(m);
                if(!modulePath.exists() || !modulePath.isDirectory()) {
                    Logger.error("Module %s will not be loaded because %s does not exist", modulePath.getName(), modulePath.getAbsolutePath());
                } else {
                    addModule(modulePath.getName(), modulePath);
                }
            }
        } else {
                for(Enumeration e = configuration.propertyNames(); e.hasMoreElements(); ) {
                String pName = e.nextElement().toString();
                if(pName.startsWith("module.")) {
                    String moduleName = pName.substring(7);
                    File modulePath = new File(configuration.getProperty(pName));
                    if(!modulePath.isAbsolute()) {
                        modulePath = new File(applicationPath, configuration.getProperty(pName));
                    }
                    if(!modulePath.exists() || !modulePath.isDirectory()) {
                        Logger.error("Module %s will not be loaded because %s does not exist", moduleName, modulePath.getAbsolutePath());
                    } else {
                        addModule(moduleName, modulePath);
                    }
                }
            }
        }
    }

    /**
     * Add a play application (as plugin)
     * @param path The application path
     */
    public static void addModule(String name, File path) {
        VirtualFile root = VirtualFile.open(path);
        modules.add(root);
        javaPath.add(root.child("app"));
        templatesPath.add(root.child("app/views"));
        modulesRoutes.put(name, root.child("conf/routes"));
        roots.add(root);
        Logger.info("Module %s is available (%s)", name, path.getAbsolutePath());
    }

    /**
     * Search a VirtualFile in all loaded applications and plugins
     * @param path Relative path from the applications root
     * @return The virtualFile or null
     */
    public static VirtualFile getVirtualFile(String path) {
        return VirtualFile.search(roots, path);
    }

    /**
     * Search a File in the current application
     * @param path Relative path from the application root
     * @return The file even if it doesn't exist
     */
    public static File getFile(String path) {
        return new File(applicationPath, path);
    }
}
