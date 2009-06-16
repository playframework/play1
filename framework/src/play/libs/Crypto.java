package play.libs;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import play.Play;
import play.exceptions.UnexpectedException;

/**
 * Crypto utils
 */
public class Crypto {

    static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String sign(String message){
        return sign(message, Play.secretKey.getBytes());
    }
    
    /**
     * Sign a message with a key
     * @param message The message to sign
     * @param key The key to use
     * @return The signed message (in hexadecimal)
     * @throws java.lang.Exception
     */
    public static String sign(String message, byte[] key) {

        if (key.length == 0) {
            return message;
        }

        byte[] messageBytes;
        KeyParameter secretKey = new KeyParameter(key);
        Digest digest = new SHA1Digest();
        HMac hmac = new HMac(digest);
        try {
            messageBytes = message.getBytes("utf-8");
            hmac.init(secretKey);
            hmac.update(messageBytes, 0, messageBytes.length);
            byte[] result = new byte[1000];
            int len = hmac.doFinal(result, 0);
            char[] hexChars = new char[len * 2];


            for (int charIndex = 0, startIndex = 0; charIndex < hexChars.length;) {
                int bite = result[startIndex++] & 0xff;
                hexChars[charIndex++] = HEX_CHARS[bite >> 4];
                hexChars[charIndex++] = HEX_CHARS[bite & 0xf];
            }
            return new String(hexChars);
        } catch (UnsupportedEncodingException ex) {
            throw new UnexpectedException(ex);
        }       

    }

    /**
     * Create a password hash
     * @param input The password
     * @return The password hash
     */
    public static String passwordHash(String input) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            byte[] out = m.digest(input.getBytes());
            return new String(Base64.encodeBase64(out));
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * Encrypt a String with the AES encryption standard using the application secret
     * @param value The String to encrypt
     * @return An hexadecimal encrypted string
     */
    public static String encryptAES(String value) {
        return encryptAES(value, Play.configuration.getProperty("application.secret").substring(0, 16));
    }

    /**
     * Encrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
     * @param value The String to encrypt
     * @param privateKey The key used to encrypt
     * @return An hexadecimal encrypted string
     */
    public static String encryptAES(String value, String privateKey) {
        try {
            byte[] raw = privateKey.getBytes();
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            return Codec.byteToHexString(cipher.doFinal(value.getBytes()));
        } catch (Exception ex) {
            throw new UnexpectedException(ex);
        }
    }

    /**
     * Decrypt a String with the AES encryption standard using the application secret
     * @param value An hexadecimal encrypted string
     * @return The decrypted String
     */
    public static String decryptAES(String value) {
        return decryptAES(value, Play.configuration.getProperty("application.secret").substring(0, 16));
    }

    /**
     * Decrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
     * @param value An hexadecimal encrypted string
     * @param privateKey The key used to encrypt
     * @return The decrypted String
     */
    public static String decryptAES(String value, String privateKey) {
        try {
            byte[] raw = privateKey.getBytes();
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            return new String(cipher.doFinal(Codec.hexStringToByte(value)));
        } catch (Exception ex) {
            throw new UnexpectedException(ex);
        }
    }
}
