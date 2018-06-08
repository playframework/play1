package play.mvc;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides operations around the encoding and decoding of Cookie data.
 */
public class CookieDataCodec {

    /**
     * Cookie session parser for cookie created by version 1.2.5 or before.
     * <p>
     * We need it to support old Play 1.2.5 session data encoding so that the cookie data doesn't become invalid when
     * applications are upgraded to a newer version of Play
     * </p>
     */
    public static Pattern oldCookieSessionParser = Pattern.compile("\u0000([^:]*):([^\u0000]*)\u0000");

    /**
     * @param map
     *            the map to decode data into.
     * @param data
     *            the data to decode.
     * @throws UnsupportedEncodingException
     *             if the encoding is not supported
     */
    public static void decode(Map<String, String> map, String data) throws UnsupportedEncodingException {
        // support old Play 1.2.5 session data encoding so that the cookie data doesn't become invalid when
        // applications are upgraded to a newer version of Play
        if (data.startsWith("%00") && data.contains("%3A") && data.endsWith("%00")) {
            String sessionData = URLDecoder.decode(data, "utf-8");
            Matcher matcher = oldCookieSessionParser.matcher(sessionData);
            while (matcher.find()) {
                map.put(matcher.group(1), matcher.group(2));
            }
            return;
        }

        String[] keyValues = data.split("&");
        for (String keyValue : keyValues) {
            String[] split = keyValue.split("=", 2);
            if (split.length == 2) {
                map.put(URLDecoder.decode(split[0], "utf-8"), URLDecoder.decode(split[1], "utf-8"));
            }
        }
    }

    /**
     * @param map
     *            the data to encode.
     * @return the encoded data.
     * @throws UnsupportedEncodingException
     *             if the encoding is not supported
     */
    public static String encode(Map<String, String> map) throws UnsupportedEncodingException {
        StringBuilder data = new StringBuilder();
        String separator = "";
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                data.append(separator).append(URLEncoder.encode(entry.getKey(), "utf-8")).append("=")
                        .append(URLEncoder.encode(entry.getValue(), "utf-8"));
                separator = "&";
            }
        }
        return data.toString();
    }

    /**
     * Constant time for same length String comparison, to prevent timing attacks
     * 
     * @param a
     *            The string a
     * @param b
     *            the string b
     * @return true is the 2 strings are equals
     */
    public static boolean safeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        } else {
            char equal = 0;
            for (int i = 0; i < a.length(); i++) {
                equal |= a.charAt(i) ^ b.charAt(i);
            }
            return equal == 0;
        }
    }
}