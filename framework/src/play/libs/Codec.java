package play.libs;

import java.io.UnsupportedEncodingException;
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
}
