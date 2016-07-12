package play.data.parsing;

import java.util.HashMap;
import java.util.Map;

public class DataParsers {
    private static final Map<String, DataParser> parsers = new HashMap<>();

    // These are our injected Parser. Maybe we later want to allow dynamic injection
    static {
        parsers.put("application/x-www-form-urlencoded", new UrlEncodedParser());
        parsers.put("multipart/form-data", new ApacheMultipartParser());
        parsers.put("multipart/mixed", new ApacheMultipartParser());
        parsers.put("application/xml", new TextParser());
        parsers.put("application/json", new TextParser());
    }

    public static DataParser forContentType(String contentType) {
        DataParser dataParser = parsers.get(contentType);
        if (dataParser != null) {
            return dataParser;
        } else if (contentType.startsWith("text/")) {
            return new TextParser();
        }
        return null;
    }
}
