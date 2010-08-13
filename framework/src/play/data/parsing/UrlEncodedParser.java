package play.data.parsing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import play.exceptions.UnexpectedException;
import play.utils.Utils;

/**
 * Parse url-encoded requests.
 */
public class UrlEncodedParser extends DataParser {
    
    boolean forQueryString = false;
    
    public static Map<String, String[]> parse(String urlEncoded) {
        try {
            return new UrlEncodedParser().parse(new ByteArrayInputStream(urlEncoded.getBytes("utf-8")));
        } catch (UnsupportedEncodingException ex) {
            throw new UnexpectedException(ex);
        }
    }
    
    public static Map<String, String[]> parseQueryString(InputStream is) {
        UrlEncodedParser parser = new UrlEncodedParser();
        parser.forQueryString = true;
        return parser.parse(is);
    }

    @Override
    public Map<String, String[]> parse(InputStream is) {
        try {
            Map<String, String[]> params = new HashMap<String, String[]>();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int b;
            while ((b = is.read()) != -1) {
                os.write(b);
            }
            byte[] data = os.toByteArray();
            // add the complete body as a parameters
            if(!forQueryString) {
                params.put("body", new String[] {new String(data, "utf-8")});
            }
            
            int ix = 0;
            int ox = 0;
            String key = null;
            String value = null;
            while (ix < data.length) {
                byte c = data[ix++];
                switch ((char) c) {
                    case '&':
                        value = new String(data, 0, ox, "utf-8");
                        if (key != null) {
                            Utils.Maps.mergeValueInMap(params, key, value);
                            key = null;
                        }
                        ox = 0;
                        break;
                    case '=':
                        if (key == null) {
                            key = new String(data, 0, ox, "utf-8");
                            ox = 0;
                        } else {
                            data[ox++] = c;
                        }
                        break;
                    case '+':
                        data[ox++] = (byte) ' ';
                        break;
                    case '%':
                        data[ox++] = (byte) ((convertHexDigit(data[ix++]) << 4) + convertHexDigit(data[ix++]));
                        break;
                    default:
                        data[ox++] = c;
                }
            }
            //The last value does not end in '&'.  So save it now.
            if (key != null) {
                value = new String(data, 0, ox, "utf-8");
                Utils.Maps.mergeValueInMap(params, key, value);
            }
            return params;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    private static byte convertHexDigit(byte b) {
        if ((b >= '0') && (b <= '9')) {
            return (byte) (b - '0');
        }
        if ((b >= 'a') && (b <= 'f')) {
            return (byte) (b - 'a' + 10);
        }
        if ((b >= 'A') && (b <= 'F')) {
            return (byte) (b - 'A' + 10);
        }
        return 0;
    }
}
