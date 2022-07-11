package play.utils;

/**
 * Os detections
 */
public class OS {

    public static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }

    public static boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac");
    }

    public static boolean isUnix() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nix") || os.contains("nux") || os.contains("aix");
    }

    public static boolean isSolaris() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("sunos");
    }
}
