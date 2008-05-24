package play.data.parsing;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public abstract class DataParser {

    public abstract Map<String, String[]> parse(InputStream is);
    
    // ~~~~~~~~ Repository 
    
    public static Map<String, DataParser> parsers = new HashMap<String, DataParser>();
    
    static {
        parsers.put("application/x-www-form-urlencoded", new UrlEncodedParser());
    }
        
}
