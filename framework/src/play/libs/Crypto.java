package play.libs;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class Crypto {

    static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String sign(String message, byte[] key) throws Exception {
        if (key.length == 0) {
            return message;
        }
        SecretKey secretKey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(secretKey);
        mac.update(message.getBytes("utf-8"));
        byte[] result = mac.doFinal();
        char[] hexChars = new char[result.length * 2];
        for (int charIndex = 0, startIndex = 0; charIndex < hexChars.length;) {
            int bite = result[startIndex++] & 0xff;
            hexChars[charIndex++] = HEX_CHARS[bite >> 4];
            hexChars[charIndex++] = HEX_CHARS[bite & 0xf];
        }
        return new String(hexChars);
    }

    public static String passwordHash(String input) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            byte[] out = m.digest(input.getBytes());
            return new String(Base64.encodeBase64(out));
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
