package play.libs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Files {
    
    public static Properties readUtf8Properties(InputStream is) throws IOException {
        Properties properties = new Properties();
        properties.load(is);
        for(String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            String goodValue = new String(value.getBytes("iso8859-1"), "utf-8");
            properties.setProperty(key, goodValue);
        }
        is.close();
        return properties;
    }

}
