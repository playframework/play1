package play.classloading;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import play.Play;

public class BytecodeCache {

    public static byte[] getBytecode(String name, String source) {
        try {
            File f = cacheFile(hash(name) + "-" + hash(source));
            if (f.exists()) {
                byte[] byteCode = new byte[(int) f.length()];
                FileInputStream fis = new FileInputStream(f);
                fis.read(byteCode);
                fis.close();
                return byteCode;
            }
            return null;
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void cacheBytecode(byte[] byteCode, String name, String source) {
        try {
            File f = cacheFile(hash(name) + "-" + hash(source));
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(byteCode);
            fos.close();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String hash(String text) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(text.getBytes("utf-8"));
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
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    static File cacheFile(String id) {
        if (!Play.getFile("work/bytecode").exists()) {
            Play.getFile("work/bytecode").mkdirs();
        }
        return Play.getFile("work/bytecode/" + id);
    }
}
