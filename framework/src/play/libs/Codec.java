package play.libs;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import play.exceptions.UnexpectedException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Codec utils
 */
public class Codec {

    /**
     * Generate an UUID String
     * 
     * @return an UUID String
     */
    public static String UUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Encode a String to base64
     * 
     * @param value
     *            The plain String
     * @return The base64 encoded String
     */
    public static String encodeBASE64(String value) {
        return java.util.Base64.getEncoder().encodeToString(value.getBytes(UTF_8));
    }

    /**
     * Encode binary data to base64
     *
     * @param value
     *            The binary data
     * @return The base64 encoded String
     */
    public static String encodeBASE64(byte[] value) {
        return java.util.Base64.getEncoder().encodeToString(value);
    }

    /**
     * Decode a base64 value
     * 
     * @param value
     *            The base64 encoded String
     * @return decoded binary data
     */
    public static byte[] decodeBASE64(String value) {
        return java.util.Base64.getDecoder().decode(value);
    }

    /**
     * Build a hexadecimal MD5 hash for a String
     * 
     * @param value
     *            The String to hash
     * @return A hexadecimal Hash
     */
    public static String hexMD5(String value) {
        return hashToHexString("MD5", value);
    }

    /**
     * Build a hexadecimal SHA1 hash for a String
     *
     * @param value
     *            The String to hash
     * @return A hexadecimal Hash
     */
    public static String hexSHA1(String value) {
        return hashToHexString("SHA-1", value);
    }

    /**
     * Write a byte array as hexadecimal String.
     *
     * @param bytes
     *            byte array
     * @return The hexadecimal String
     */
    public static String byteToHexString(byte[] bytes) {
        return String.valueOf(Hex.encodeHex(bytes));
    }

    /**
     * Transform a hexadecimal String to a byte array.
     *
     * @param hexString
     *            Hexadecimal string to transform
     * @return The byte array
     */
    public static byte[] hexStringToByte(String hexString) {
        try {
            return Hex.decodeHex(hexString.toCharArray());
        } catch (DecoderException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Build a hexadecimal hash specified by {@code algorithm} for a given {@code value}.
     *
     * @param algorithm The hash name
     * @param value     The String to hash
     *
     * @return A hexadecimal hash
     */
    private static String hashToHexString(String algorithm, String value) {
        try {
            return byteToHexString(
                MessageDigest.getInstance(algorithm)
                    .digest(value.getBytes(UTF_8))
            );
        } catch (NoSuchAlgorithmException ex) {
            throw new UnexpectedException(ex);
        }
    }

}
