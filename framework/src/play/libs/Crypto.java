package play.libs;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {

    static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String sign(String message, byte[] key) throws Exception {
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
}
