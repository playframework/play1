package play;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import play.classloading.ApplicationClasses;
import play.classloading.ApplicationClassloader;

public class Play {

    public static ApplicationClasses classes = new ApplicationClasses();
    public static ApplicationClassloader classloader;
    public static List<File> javaPath;

    public static synchronized void start() {
        // Build the current JavaPath
        javaPath = new ArrayList<File>();
        javaPath.add(new File("app"));
        // Start a fresh classloader
        classloader = new ApplicationClassloader();
    }

    public static synchronized void detectChanges() {
        try {
            classloader.detectChanges();
        } catch (UnsupportedOperationException e) {
            // We have to do a clean refresh
            start();
        }
    }
}
