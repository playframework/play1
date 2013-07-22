package play.data.parsing;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A data parser parse the HTTP request data to a Map<String,String[]>
 */
public abstract class DataParser {

    // ~~~~~~~~ Repository 
    public abstract Map<String, String[]> parse(InputStream is);    
    public static Map<String, DataParser> parsers = new HashMap<String, DataParser>();    

    // These are our injected Parser. Maybe we later want to allow dynamic injection
    static {
        parsers.put("application/x-www-form-urlencoded", new UrlEncodedParser());
        parsers.put("multipart/form-data", new ApacheMultipartParser());
        parsers.put("multipart/mixed", new ApacheMultipartParser());
        parsers.put("application/xml", new TextParser());
        parsers.put("application/json", new TextParser());
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
