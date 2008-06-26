package play.libs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

public class IO {
    
    public static Properties readUtf8Properties(InputStream is) throws IOException {
        Properties properties = new Properties();
        properties.load(is);
        for(Object key : properties.keySet()) { 
            String value = properties.getProperty(key.toString());
            String goodValue = new String(value.getBytes("iso8859-1"), "utf-8");
            properties.setProperty(key.toString(), goodValue);
        }
        is.close();
        return properties;
    }
    
    public static String readContentAsString(InputStream is) throws IOException {
        StringWriter result = new StringWriter();
        PrintWriter out = new PrintWriter(result);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
        String line = null;
        while((line = reader.readLine())!= null) {
            out.println(line);                    
        }
        is.close();
        return result.toString();        
    }
    
    public static void writeContent(CharSequence content, OutputStream os) throws IOException {
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(os, "utf-8"));
        printWriter.println(content);
        printWriter.flush();
        os.flush();
        os.close();
    }

}
