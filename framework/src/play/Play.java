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
import java.util.List;
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
     * All paths to search for Java files
     */
    public static List<VirtualFile> javaPath;
    /**
     * All paths to search for templates files
     */
    public static List<VirtualFile> templatesPath;
    /**
     * All routes files
     */
    public static List<VirtualFile> routes;
    /**
     * All paths to search for static resources
     */
    public static List<VirtualFile> staticResources;
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
        Logger.info("Starting %s", root.getAbsolutePath());
        // Read the configuration file
        readConfiguration();
        // Build basic java source path
        VirtualFile appRoot = VirtualFile.open(applicationPath);
        javaPath = new ArrayList<VirtualFile>();
        javaPath.add(appRoot.child("app"));
        javaPath.add(appRoot.child("test"));
        // Build basic templates path
        templatesPath = new ArrayList<VirtualFile>();
        templatesPath.add(appRoot.child("app/views"));
        templatesPath.add(VirtualFile.open(new File(frameworkPath, "framework/templates")));
        // Build basic static resources path
        staticResources = new ArrayList<VirtualFile>();
        staticResources.add(appRoot.child("public"));
        // Main route file
        routes = new ArrayList<VirtualFile>();
        routes.add(appRoot.child("conf/routes"));
        // Enable a first classloader
        classloader = new ApplicationClassloader();
        // Plugins
        bootstrapPlugins();
        // Mode
        mode = Mode.valueOf(configuration.getProperty("application.mode", "DEV").toUpperCase());
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
            // Need a new classloader
            classloader = new ApplicationClassloader();
            Thread.currentThread().setContextClassLoader(Play.classloader);
            // Reload plugins
            List<PlayPlugin> newPlugins = new ArrayList<PlayPlugin> ();
            for (PlayPlugin plugin : plugins) {
            	if (plugin.getClass().getClassLoader().getClass().equals(ApplicationClassloader.class)) {
            		PlayPlugin newPlugin = (PlayPlugin) classloader.loadClass(plugin.getClass().getName()).getConstructors()[0].newInstance();
            		newPlugin.onLoad();
            		newPlugins.add(newPlugin);
            	} else
            		newPlugins.add(plugin);
            }
            plugins = newPlugins;
            // Reload configuration
            readConfiguration();
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
            // Routes definitions            
            Router.load();

            // Try to load all classes
            Play.classloader.getAllClasses();                    

            // Plugins
            for (PlayPlugin plugin : plugins) {
                plugin.onApplicationStart();
            }

            // We made it
            started = true;
            startedAt = System.currentTimeMillis();
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
    }

    static void preCompile() {
        try {
            Logger.info("Precompiling ...");
            classloader.getAllClasses();
            TemplateLoader.getAllTemplate();
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
    public static void bootstrapPlugins() {
        // Classic modules
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
                    plugin.index=Integer.parseInt(infos[0]);
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
        //Auto load things in lib
        File lib = new File(applicationPath, "lib");
        File[] libs = lib.listFiles();
        if (libs != null) {
            for (int i = 0; i < libs.length; i++) {
                if (libs[i].isFile() && (libs[i].toString().endsWith(".zip"))) {
                    addPlayApp(libs[i]);
                } else if (isPlayApp(libs[i])) {
                    addPlayApp(libs[i]);
                }
            }
        }
        //Load specific
        String pluginPath = configuration.getProperty("plugin.path");
        String[] pluginNames = configuration.getProperty("plugin.enable", "").split(",");
        if ("".equals(pluginNames[0])) {
            return;
        }
        for (int i = 0; i < pluginNames.length; i++) {
            String pluginName = pluginNames[i];
            File fl = new File(pluginName);
            if (fl.isFile() && (fl.toString().endsWith(".zip"))) {
                addPlayApp(fl);
            } else {
                if (fl.isAbsolute() && isPlayApp(fl)) {
                    addPlayApp(fl);
                } else {
                    fl = new File(new File(pluginPath), fl.getPath());
                    if (fl.isAbsolute() && isPlayApp(fl)) {
                        addPlayApp(fl);
                    } else {
                        new RuntimeException(fl.getAbsolutePath() + " is not a play application/plugin !");
                    }
                }
            }
        }
    }

    /**
     * Add a play application (as plugin)
     * @param path The application path
     */
    public static void addPlayApp(File path) {
        VirtualFile root = VirtualFile.open(path);
        javaPath.add(root.child("app"));
        templatesPath.add(root.child("app/views"));
        staticResources.add(root.child("public"));
        routes.add(root.child("conf/routes"));
        Logger.info("Plugin added: " + path.getAbsolutePath());
    }

    /**
     * Guess if the path contains a valid application
     * @param path The application path
     * @return It depends
     */
    public static boolean isPlayApp(File path) {
        if (!(new File(path, "app").exists())) {
            return false;
        }
        if (!(new File(path, "app").isDirectory())) {
            return false;
        }
        if (!(new File(path, "app/controllers/").exists())) {
            return false;
        }
        if (!(new File(path, "app/models/").exists())) {
            return false;
        }
        if (!(new File(path, "conf/routes").exists())) {
            return false;
        }
        return true;
    }

    /**
     * Search a VirtualFile in all loaded applications and plugins
     * @param path Relative path from the applications root
     * @return The virtualFile or null
     */
    public static VirtualFile getVirtualFile(String path) {
        return VirtualFile.open(applicationPath).child(path);
    }

    /**
     * Search a File in the current application
     * @param path Relative path from the application root
     * @return The fiel even if it doesn't exist
     */
    public static File getFile(String path) {
        return new File(applicationPath, path);
    }
}
