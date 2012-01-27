package play.data.parsing;

import java.io.InputStream;
import java.util.Map;

public class DirectStreamingParser extends DataParser {

    @Override
    public Map<String, String[]> parse(InputStream is) {
        throw new UnsupportedOperationException("Requests exposing a direct InputStream should not be parsed.");
    }

}