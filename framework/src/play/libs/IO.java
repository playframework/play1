package play.libs;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

/**
 * IO utils
 */
public class IO {

    /**
     * Read a properties file with the utf-8 encoding
     * @param is Stream to properties file
     * @return The Properties object
     * @throws java.io.IOException
     */
    public static Properties readUtf8Properties(InputStream is) throws IOException {
        Properties properties = new Properties();
        properties.load(is);
        for (Object key : properties.keySet()) {
            String value = properties.getProperty(key.toString());
            String goodValue = new String(value.getBytes("iso8859-1"), "utf-8");
            properties.setProperty(key.toString(), goodValue);
        }
        is.close();
        return properties;
    }

    /**
     * Read the Stream conten as a string (always use utf-8)
     * @param is The stream to read
     * @return The String content
     * @throws java.io.IOException
     */
    public static String readContentAsString(InputStream is) throws IOException {
        String res = IOUtils.toString(is, "utf-8");
        is.close();
        return res;
    }

    /**
     * Read file content to a String (always use utf-8)
     * @param file The file to read
     * @return The String content
     * @throws java.io.IOException
     */
    public static String readContentAsString(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        StringWriter result = new StringWriter();
        PrintWriter out = new PrintWriter(result);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
        String line = null;
        while ((line = reader.readLine()) != null) {
            out.println(line);
        }
        is.close();
        return result.toString();
    }

    /**
     * Read binary content of a file (warning does not use on large file !)
     * @param file The file te read
     * @return The binary data
     * @throws java.io.IOException
     */
    public static byte[] readContent(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        byte[] result = new byte[(int) file.length()];
        is.read(result);
        is.close();
        return result;
    }
    
    public static byte[] readContent(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int read = 0;
        byte[] buffer = new byte[8096];
        while((read = is.read(buffer)) > 0) {
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }

    /**
     * Write String content to a stream (always use utf-8)
     * @param content The content to write
     * @param os The stream to write
     * @throws java.io.IOException
     */
    public static void writeContent(CharSequence content, OutputStream os) throws IOException {
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(os, "utf-8"));
        printWriter.println(content);
        printWriter.flush();
        os.flush();
        os.close();
    }

    /**
     * Write String content to a file (always use utf-8)
     * @param content The content to write
     * @param file The file to write
     * @throws java.io.IOException
     */
    public static void writeContent(CharSequence content, File file) throws IOException {
        OutputStream os = new FileOutputStream(file);
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(os, "utf-8"));
        printWriter.println(content);
        printWriter.flush();
        os.flush();
        os.close();
    }
    
    /**
     * Write binay data to a file
     * @param data The binary data to write
     * @param file The file to write
     * @throws java.io.IOException
     */
    public static void write(byte[] data, File file) throws IOException {
        OutputStream os = new FileOutputStream(file);
        os.write(data);
        os.flush();
        os.close();
    }
}
