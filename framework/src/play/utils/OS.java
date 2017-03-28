package play.utils;

/**
 * Os detections
 */
public class OS {

    public static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os != null && os.indexOf("win") >= 0;
    }

    public static boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return os != null && (os.toLowerCase().indexOf("mac") >= 0);
    }

    public static boolean isUnix() {
        String os = System.getProperty("os.name").toLowerCase();
        return os != null && (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os.indexOf("aix") > 0);
    }

    public static boolean isSolaris() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os != null && os.indexOf("sunos") >= 0);
    }
}
