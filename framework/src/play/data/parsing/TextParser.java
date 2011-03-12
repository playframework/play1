package play.data.parsing;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import play.exceptions.UnexpectedException;

public class TextParser extends DataParser {

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
            params.put("body", new String[] {new String(data, "utf-8")});
            return params;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

}
