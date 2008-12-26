package play.classloading;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import play.Logger;
import play.Play;

/**
 * Used to speed up compilation time
 */
public class BytecodeCache {

    // Please update the cache version at each release
    static String version = "7";
    
    /**
     * Delete the bytecode
     * @param name Cache name
     */
    public static void deleteBytecode(String name) {
        try {
            if (!Play.configuration.getProperty("play.bytecodeCache", "true").equals("true")) {
                return;
            }
            File f = cacheFile(name.replace("/", "_"));
            if (f.exists()) {
                f.delete();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve the bytecode if source has not changed
     * @param name The cache name
     * @param source The source code
     * @return The bytecode
     */
    public static byte[] getBytecode(String name, String source) {
        try {
            if (!Play.configuration.getProperty("play.bytecodeCache", "true").equals("true")) {
                return null;
            }
            File f = cacheFile(name.replace("/", "_"));
            if (f.exists()) {
                FileInputStream fis = new FileInputStream(f);
                // Read hash
                int offset = 0;
                int read = -1;
                StringBuilder hash = new StringBuilder();
                while ((read = fis.read()) != 0) {
                    hash.append((char) read);
                    offset++;
                }
                if (!hash(source).equals(hash.toString())) {
                    Logger.trace("Bytecode too old (%s != %s)", hash, hash(source));
                    return null;
                }
                byte[] byteCode = new byte[(int) f.length() - (offset + 1)];
                fis.read(byteCode);
                fis.close();
                return byteCode;
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Cache the bytecode
     * @param byteCode The bytecode
     * @param name The cache name
     * @param source The corresponding source
     */
    public static void cacheBytecode(byte[] byteCode, String name, String source) {
        try {
            if (!Play.configuration.getProperty("play.bytecodeCache", "true").equals("true")) {
                return;
            }
            File f = cacheFile(name.replace("/", "_"));
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(hash(source).getBytes("utf-8"));
            fos.write(0);
            fos.write(byteCode);
            fos.close();
            Logger.trace("%s cached", name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String hash(String text) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update((version + text).getBytes("utf-8"));
            byte[] digest = messageDigest.digest();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < digest.length; ++i) {
                int value = digest[i];
                if (value < 0) {
                    value += 256;
                }
                builder.append(Integer.toHexString(value));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static File cacheFile(String id) {
        File dir = new File(Play.tmpDir, "bytecode/" + Play.mode.name());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, id);
    }
}
