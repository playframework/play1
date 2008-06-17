package play.libs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

public class Files {
    
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
    
    public static void writeContent(String content, File toFile) throws IOException {
        OutputStream os = new FileOutputStream(toFile);
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(os, "utf-8"));
        printWriter.println(content);
        printWriter.flush();
        os.close();
    }
    
    public static void appendContent(String content, File toFile) throws IOException {
        OutputStream os = new FileOutputStream(toFile, true);
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(os, "utf-8"));
        printWriter.println(content);
        printWriter.flush();
        os.close();
    }

}
