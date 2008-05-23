package play;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import java.util.Properties;
import play.classloading.ApplicationClasses;
import play.classloading.ApplicationClassloader;
import play.libs.Files;
import play.mvc.Router;

public class Play {
    
    // Internal
    public static boolean started = false;
    
    // Application
    public static File root = null;            
    public static ApplicationClasses classes = new ApplicationClasses();
    public static ApplicationClassloader classloader;
    public static List<VirtualFile> javaPath;
    public static Properties configuration;
    public static String applicationName;
    
    public static void init(File root) {
        Play.started = false;
        Play.root = root;
        start();
    }

    public static synchronized void start() {
        try {
            started = false;
            // 1. Configuration
            configuration = Files.readUtf8Properties(new VirtualFile("conf/application.conf").inputstream());
            applicationName = configuration.getProperty("application.name", "(no name)");
            // 2. The Java path
            javaPath = new ArrayList<VirtualFile>();
            javaPath.add(new VirtualFile("app"));
            // 3. The classloader
            classloader = new ApplicationClassloader();
            // 4. Routes
            Router.load(new VirtualFile("conf/routes"));
            // Ok
            started = true;
            Logger.info("Application %s is started !", applicationName);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized void detectChanges() {
        try {
            classloader.detectChanges();
        } catch (UnsupportedOperationException e) {
            // We have to do a clean refresh
            start();
        }
    }
    
    public static class VirtualFile {
        
        File realFile;
        
        public VirtualFile(String path) {   
            realFile = new File(root, path);
        }
        
        public VirtualFile(VirtualFile parent, String path) {         
            if(parent.realFile != null) {
                realFile = new File(parent.realFile, path);
            }
        }
        
        public boolean exists() {
            if(realFile != null) {
                return realFile.exists();
            }
            return false;
        }
        
        public InputStream inputstream() {
            if(realFile != null) {
                try {
                    return new FileInputStream(realFile);
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        }
        
        public String contentAsString() {
            try {
                return new String(content(), "utf-8");
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        public byte[] content() {
            if(realFile != null) {
                byte[] buffer = new byte[(int)realFile.length()];
                try {
                    InputStream is = inputstream();
                    is.read(buffer);
                    is.close();
                    return buffer;
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        }
        
        public Long lastModified() {
            if(realFile != null) {
                return realFile.lastModified();
            }
            return 0L;
        }
        
    }
}
