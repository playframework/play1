package play.data.parsing;

import java.io.InputStream;
import java.util.Map;

/**
 * A data parser parse the HTTP request data to a Map<String,String[]>
 */
public abstract class DataParser {

    public abstract Map<String, String[]> parse(InputStream is);

}
