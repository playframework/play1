package play.data.parsing;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import play.Logger;
import play.mvc.Http.Request;

/**
 * A data parser parse the HTTP request data to a Map<String,String[]>
 */
public abstract class DataParser {

    public static Map<String, String[]> resolveAndParse(Request request) throws UnsupportedEncodingException {
        // 1. determine content-type in request
        Logger.info("request contentType is %s", request.contentType);
        String mimeType = request.contentType;
        if (mimeType.indexOf(';') != -1) {
            mimeType = mimeType.substring(0, mimeType.indexOf(';'));
        }
        DataParser parser = parsers.get(mimeType);
        if (parser == null) {
            Logger.warn("no parser registered for content type %s", mimeType);
            parser = parsers.get("application/x-www-form-urlencoded");
        }
        return parser.parse(new ByteArrayInputStream(request.querystring.getBytes("utf-8")));
    }

    // ~~~~~~~~ Repository 
    public abstract Map<String, String[]> parse(InputStream is);    
    public static Map<String, DataParser> parsers = new HashMap<String, DataParser>();
    

    static {
        parsers.put("application/x-www-form-urlencoded", new UrlEncodedParser());
        parsers.put("multipart/form-data", new ApacheMultipartParser());
        JsonParser jsonparser = new JsonParser();
        // this is the official json mimetype as defined in RFC4627
        parsers.put("application/json", jsonparser);
        parsers.put("application/javascript", jsonparser);
    }

    public static void putMapEntry(Map<String, String[]> map, String name, String value) {
        String[] newValues = null;
        String[] oldValues = map.get(name);
        if (oldValues == null) {
            newValues = new String[1];
            newValues[0] = value;
        } else {
            newValues = new String[oldValues.length + 1];
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
            newValues[oldValues.length] = value;
        }
        map.put(name, newValues);
    }
}
