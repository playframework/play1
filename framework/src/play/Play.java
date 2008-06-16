package play;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.Properties;
import play.classloading.ApplicationClasses;
import play.classloading.ApplicationClassloader;
import play.db.DB;
import play.db.jpa.JPA;
import play.exceptions.UnexpectedException;
import play.libs.Files;
import play.mvc.Router;
import play.templates.TemplateLoader;

public class Play {
    
    // Internal
    public static boolean started = false;
    
    // Application
    public static File applicationPath = null;  
    public static File frameworkPath = null;
    public static ApplicationClasses classes = new ApplicationClasses();
    public static ApplicationClassloader classloader;
    public static List<VirtualFile> javaPath;
    public static List<VirtualFile> templatesPath;
    public static List<VirtualFile> routes;
    public static Properties configuration;
    public static String applicationName;
    
    public static void init(File root) {
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
        start();
    }

    public static synchronized void start() {
        try {
            long start = System.currentTimeMillis();
            if(started) {
                stop();
            }
            Thread.currentThread().setContextClassLoader(Play.classloader);
            configuration = Files.readUtf8Properties(new VirtualFile("conf/application.conf").inputstream());
            applicationName = configuration.getProperty("application.name", "(no name)");
            javaPath = new ArrayList<VirtualFile>();
            javaPath.add(new VirtualFile("app"));
            templatesPath = new ArrayList<VirtualFile>();
            templatesPath.add(new VirtualFile("app/views"));
            templatesPath.add(new VirtualFile(new File(frameworkPath , "framework")));
            classloader = new ApplicationClassloader();
            routes=new ArrayList<Play.VirtualFile>();
            routes.add(new VirtualFile("conf/routes"));
            Router.load();
            TemplateLoader.cleanCompiledCache();
            DB.init();
            JPA.init();
            started = true;
            Logger.debug("%sms to start the application", System.currentTimeMillis()-start);
            Logger.info("Application '%s' is started !", applicationName);
        } catch(Exception e) {
            throw new UnexpectedException(e);
        }
    }
    
    public static synchronized void stop() {
        JPA.shutdown();
        started = false;
    }
   
    protected static synchronized void detectChanges() {
        try {
            Router.detectChanges();
            classloader.detectChanges();            
        } catch (UnsupportedOperationException e) {
            // We have to do a clean refresh
            start();
        }
    }
    
    public static String getSecretKey() {
        return "SLD0FBVG78920DKMLKF39DJ92JO2";
    }
    
    public static VirtualFile getFile(String path) {
        return new VirtualFile(path);
    }
    
    public static class VirtualFile {

        File realFile;
        
        public VirtualFile(String path) {
            realFile = new File(applicationPath, path);
        }

        public VirtualFile(File file) {
            realFile = file;
        }

        public String getName() {
            if (realFile != null) {
                return realFile.getName();
            } else {
                throw new UnsupportedOperationException();
            }
        }

        public boolean isDirectory() {
            if (realFile != null) {
                return realFile.isDirectory();
            } else {
                throw new UnsupportedOperationException();
            }
        }

        public String relativePath() {
            if (realFile != null) {
                List<String> path = new ArrayList<String>();
                File f = realFile;
                while(f != null && !f.equals(Play.applicationPath) && !f.equals(Play.frameworkPath)) {
                    path.add(f.getName());
                    f = f.getParentFile();
                }
                Collections.reverse(path);
                StringBuilder builder = new StringBuilder();
                for(String p : path) {
                    builder.append("/"+p);
                }
                return builder.toString();
            }
            return null;
        }

        public VirtualFile get(String path) {
            if (realFile != null) {
                return new VirtualFile(new File(realFile, path));
            }
            return null;
        }

        public List<VirtualFile> list() {
            List<VirtualFile> res = new ArrayList<VirtualFile>();
            if (realFile != null) {
                File[] children = realFile.listFiles();
                for (int i = 0; i < children.length; i++) {
                    res.add(new VirtualFile(children[i]));
                }
            } else {
                throw new UnsupportedOperationException();
            }
            return res;
        }

        public VirtualFile(VirtualFile parent, String path) {
            if (parent.realFile != null) {
                realFile = new File(parent.realFile, path);
            }
        }

        public boolean exists() {
            if (realFile != null) {
                return realFile.exists();
            }
            return false;
        }

        public InputStream inputstream() {
            if (realFile != null) {
                try {
                    return new FileInputStream(realFile);
                } catch (Exception e) {
                    throw new UnexpectedException(e);
                }
            }
            return null;
        }

        public String contentAsString() {
            try {
                return Files.readContentAsString(inputstream());
            } catch (Exception e) {
                throw new UnexpectedException(e);
            }
        }

        public byte[] content() {
            if (realFile != null) {
                byte[] buffer = new byte[(int) realFile.length()];
                try {
                    InputStream is = inputstream();
                    is.read(buffer);
                    is.close();
                    return buffer;
                } catch (Exception e) {
                    throw new UnexpectedException(e);
                }
            }
            return null;
        }

        public Long lastModified() {
            if (realFile != null) {
                return realFile.lastModified();
            }
            return 0L;
        }

        @Override
        public boolean equals(Object other) {
            if(other instanceof VirtualFile) {
                VirtualFile vf = (VirtualFile)other;
                if(realFile != null && vf.realFile != null) {
                    return realFile.equals(vf.realFile);
                }
            }
            return super.equals(other);
        }

        @Override
        public int hashCode() {
            if(realFile != null) {
                return realFile.hashCode();
            }
            return super.hashCode();
        }
        
        
        
    }
}
