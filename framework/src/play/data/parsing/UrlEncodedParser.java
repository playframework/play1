package play.data.parsing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.utils.Utils;

/**
 * Parse url-encoded requests.
 */
public class UrlEncodedParser extends DataParser {
    
    boolean forQueryString = false;
    
    public static Map<String, String[]> parse(String urlEncoded) {
        try {
            final String encoding = Http.Request.current().encoding;
            return new UrlEncodedParser().parse(new ByteArrayInputStream(urlEncoded.getBytes( encoding )));
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
        final String encoding = Http.Request.current().encoding;
        try {
            Map<String, String[]> params = new HashMap<String, String[]>();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ( (bytesRead = is.read(buffer)) > 0 ) {
                os.write( buffer, 0, bytesRead);
            }

            String data = new String(os.toByteArray(), encoding);
            // add the complete body as a parameters
            if(!forQueryString) {
                params.put("body", new String[] {data});
            }

            // data is o the form:
            // a=b&b=c%12...
            String[] keyValues = data.split("&");
            for (String keyValue : keyValues) {
                // split this key-value on '='
                String[] parts = keyValue.split("=");
                // sanity check
                if (parts.length >= 1) {
                    String key = URLDecoder.decode(parts[0],encoding);
                    if (key.length()>0) {
                        String value = null;
                        if (parts.length == 2) {
                            value = URLDecoder.decode(parts[1],encoding);
                        } else {
                            // if keyValue ends with "=", then we have an empty value
                            // if not ending with "=", we have a key without a value (a flag)
                            if (keyValue.endsWith("=")) {
                                value = "";
                            }
                        }
                        Utils.Maps.mergeValueInMap(params, key, value);
                    }
                }
            }
            return params;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

}
