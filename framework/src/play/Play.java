package play;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Level;
import play.cache.Cache;
import play.classloading.ApplicationClasses;
import play.classloading.ApplicationClassloader;
import play.db.DB;
import play.db.jpa.JPA;
import play.exceptions.UnexpectedException;
import play.i18n.Messages;
import play.libs.IO;
import play.vfs.VirtualFile;
import play.mvc.Router;
import play.templates.TemplateLoader;
import zdb.core.Store;

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
     * The application name (from application.name)
     */
    public static String applicationName;
    
    /**
     * The last time than the application has started
     */
    public static Long startedAt;
    
    /**
     * The list of supported locales
     */
    public static List<String> locales;
    
    /**
     * The very secret key
     */
    public static String secretKey;

    /**
     * Init the framework
     * @param root The application path
     * @param id The framework id to use
     */
    public static void init(File root, String id) {
        Play.id = id;
        Play.started = false;
        Play.applicationPath = root;
        try {
            URI uri = Play.class.getResource("/play/version").toURI();
            if (uri.getScheme().equals("jar")) {
                String jarPath = uri.getSchemeSpecificPart().substring(5, uri.getSchemeSpecificPart().lastIndexOf("!"));
                frameworkPath = new File(jarPath).getParentFile().getParentFile().getAbsoluteFile();
            } else if (uri.getScheme().equals("file")) {
                frameworkPath = new File(uri).getParentFile().getParentFile().getParentFile().getParentFile();
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
        Logger.info("Starting %s", root.getAbsolutePath());
        start();
        if (mode == Mode.DEV) {
            Logger.warn("You're running Play! in DEV mode");
        }
        Logger.info("Application '%s' is ready !", applicationName);
    }

    /**
     * Start the application.
     * Recall to restart !
     */
    public static synchronized void start() {
        try {
            long start = System.currentTimeMillis();
            if (started) {
                Logger.debug("Reloading ...");
                stop();
            }
            Thread.currentThread().setContextClassLoader(Play.classloader);
            VirtualFile appRoot = VirtualFile.open(applicationPath);
            conf = appRoot.child("conf/application.conf");
            configuration = IO.readUtf8Properties(conf.inputstream());
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
            // XLog
            String logLevel = configuration.getProperty("application.log", "INFO");
            Logger.log4j.setLevel(Level.toLevel(logLevel));
            // Locales
            locales = Arrays.asList(configuration.getProperty("application.langs", "").split(","));
            if(locales.size() == 1 && locales.get(0).trim().equals("")) {
                locales = new ArrayList<String>();
            }
            // Application name
            applicationName = configuration.getProperty("application.name", "(no name)");
            // Mode
            mode = Mode.valueOf(configuration.getProperty("application.mode", "DEV").toUpperCase());
            // Cache
            Cache.init();
            // Java source path
            javaPath = new ArrayList<VirtualFile>();
            javaPath.add(appRoot.child("app"));
            javaPath.add(appRoot.child("test"));
            // Templates path
            templatesPath = new ArrayList<VirtualFile>();
            templatesPath.add(appRoot.child("app/views"));
            templatesPath.add(VirtualFile.open(new File(frameworkPath, "framework/templates")));
            TemplateLoader.cleanCompiledCache();
            //Static resources
            staticResources = new ArrayList<VirtualFile>();
            staticResources.add(appRoot.child("public"));
            // Classloader
            classloader = new ApplicationClassloader();
            // SecretKey
            secretKey = configuration.getProperty("application.secret", "").trim();
            if(secretKey.equals("")) {
                Logger.warn("No secret key defined. Sessions will not be encrypted");
            }
            // ZDB
            if(configuration.getProperty("zdb", "disabled").equals("enabled")) {
                Store.init(new File(applicationPath, "zdb"));
            }
            // Routes definitions
            routes = new ArrayList<VirtualFile>();
            routes.add(appRoot.child("conf/routes"));
            Router.load();
            // Init messages
            Messages.load();
            // Plugins
            bootstrapPlugins();
            DB.init();
            JPA.init();
            // PROD mode
            if(mode == Mode.PROD) {
                preCompile();
            }
            // Yop
            started = true;
            Logger.trace("%sms to start the application", System.currentTimeMillis() - start);
            startedAt = System.currentTimeMillis();
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Stop the application
     */
    public static synchronized void stop() {
        JPA.shutdown();
        started = false;
    }
    
    static void preCompile() {
        try {
            Logger.info("Precompiling ...");
            classloader.getAllClasses();
            TemplateLoader.getAllTemplate();
        } catch(Throwable e) {
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
            if (conf.lastModified() > startedAt) {
                start();
                return;
            }
            Router.detectChanges();
            classloader.detectChanges();
            Messages.detectChanges();
        } catch (UnsupportedOperationException e) {
            // We have to do a clean refresh
            start();
        }
    }

    /**
     * Enable found plugins
     * @throws java.io.IOException
     */
    public static void bootstrapPlugins() throws IOException {
        //Auto load things in lib
    	File lib = new File(applicationPath, "lib");
        File[] libs = lib.listFiles();
        if (libs!=null) {
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
