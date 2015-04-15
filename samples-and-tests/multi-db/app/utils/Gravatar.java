package utils;

import java.security.MessageDigest;
import java.math.BigInteger;
import play.templates.JavaExtensions;

public class Gravatar extends JavaExtensions {

	
    public static String gravatar(String s) throws Exception {
	String digest = toHexString(MessageDigest.getInstance("MD5").digest(
		s.getBytes()));
	return "http://www.gravatar.com/avatar/" + digest.toLowerCase();
    }

    private static String toHexString(byte[] buf) {
	BigInteger bi = new BigInteger(1, buf);
	return String.format("%0" + (buf.length << 1) + "X", bi);
    }
}