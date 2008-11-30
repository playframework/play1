package play.libs;

import com.sun.java_cup.internal.version;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;
import play.exceptions.UnexpectedException;

public class Codec {

    public static String UUID() {
        return UUID.randomUUID().toString();
    }

    public static String encodeBASE64(String value) {
        try {
            return new String(Base64.encodeBase64(value.getBytes("utf-8")));
        } catch (UnsupportedEncodingException ex) {
            throw new UnexpectedException(ex);
        }
    }

    public static String encodeBASE64(byte[] value) {
        return new String(Base64.encodeBase64(value));
    }

    public static byte[] decodeBASE64(String value) {
        try {
            return Base64.decodeBase64(value.getBytes("utf-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new UnexpectedException(ex);
        }
    }

    public static String hexMD5(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(value.getBytes("utf-8"));
            byte[] digest = messageDigest.digest();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < digest.length; ++i) {
                int v = digest[i];
                if (v < 0) {
                    v += 256;
                }
                builder.append(Integer.toHexString(v));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new UnexpectedException(ex);
        }
    }
}
