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
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import play.exceptions.UnexpectedException;

/**
 * IO utils
 */
public class IO {

    /**
     * Read a properties file with the utf-8 encoding
     * @param is Stream to properties file
     * @return The Properties object
     */
    public static Properties readUtf8Properties(InputStream is) {
        Properties properties = new Properties();
        try {
            properties.load(is);
            for (Object key : properties.keySet()) {
                String value = properties.getProperty(key.toString());
                String goodValue = new String(value.getBytes("iso8859-1"), "utf-8");
                properties.setProperty(key.toString(), goodValue);
            }
            is.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return properties;
    }

    /**
     * Read the Stream content as a string (use utf-8)
     * @param is The stream to read
     * @return The String content
     */
    public static String readContentAsString(InputStream is) {
        return readContentAsString(is, "utf-8");
    }

    /**
     * Read the Stream content as a string
     * @param is The stream to read
     * @return The String content
     */
    public static String readContentAsString(InputStream is, String encoding) {
        String res = null;
        try {
            res = IOUtils.toString(is, encoding);
            is.close();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        return res;
    }
    /**
     * Read file content to a String (always use utf-8)
     * @param file The file to read
     * @return The String content
     */
    public static String readContentAsString(File file) {
        return readContentAsString(file, "utf-8");
    }

    /**
     * Read file content to a String
     * @param file The file to read
     * @return The String content
     */
    public static String readContentAsString(File file, String encoding) {
        try {
            InputStream is = new FileInputStream(file);
            StringWriter result = new StringWriter();
            PrintWriter out = new PrintWriter(result);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, encoding));
            String line = null;
            while ((line = reader.readLine()) != null) {
                out.println(line);
            }
            is.close();
            return result.toString();
        } catch(IOException e) {
            throw new UnexpectedException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<String> readLines(InputStream is) {
        List<String> lines = null;
        try {
            lines = IOUtils.readLines(is);
        } catch (IOException ex) {
            throw new UnexpectedException(ex);
        }
        return lines;
    }

    @SuppressWarnings("unchecked")
    public static List<String> readLines(File file, String encoding) {
        List<String> lines = null;
        try {
            InputStream is = new FileInputStream(file);
            lines = IOUtils.readLines(is, encoding);
            is.close();
        } catch (IOException ex) {
            throw new UnexpectedException(ex);
        }
        return lines;
    }

    public static List<String> readLines(File file) {
        return readLines(file, "utf-8");
    }

    /**
     * Read binary content of a file (warning does not use on large file !)
     * @param file The file te read
     * @return The binary data
     */
    public static byte[] readContent(File file) {
        try {
            InputStream is = new FileInputStream(file);
            byte[] result = new byte[(int) file.length()];
            is.read(result);
            is.close();
            return result;
        } catch(IOException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Read binary content of a stream (warning does not use on large file !)
     * @param is The stream to read
     * @return The binary data
     */
    public static byte[] readContent(InputStream is) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int read = 0;
            byte[] buffer = new byte[8096];
            while ((read = is.read(buffer)) > 0) {
                baos.write(buffer, 0, read);
            }
            return baos.toByteArray();
        } catch(IOException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Write String content to a stream (always use utf-8)
     * @param content The content to write
     * @param os The stream to write
     */
    public static void writeContent(CharSequence content, OutputStream os) {
        writeContent(content, os, "utf-8");
    }

    /**
     * Write String content to a stream (always use utf-8)
     * @param content The content to write
     * @param os The stream to write
     */
    public static void writeContent(CharSequence content, OutputStream os, String encoding) {
        try {
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(os, encoding));
            printWriter.println(content);
            printWriter.flush();
            os.flush();
            os.close();
        } catch(IOException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Write String content to a file (always use utf-8)
     * @param content The content to write
     * @param file The file to write
     */
    public static void writeContent(CharSequence content, File file) {
        writeContent(content, file, "utf-8");
    }

    /**
     * Write String content to a file (always use utf-8)
     * @param content The content to write
     * @param file The file to write
     */
    public static void writeContent(CharSequence content, File file, String encoding) {
        try {
            OutputStream os = new FileOutputStream(file);
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(os, encoding));
            printWriter.println(content);
            printWriter.flush();
            os.flush();
            os.close();
        } catch(IOException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Write binay data to a file
     * @param data The binary data to write
     * @param file The file to write
     */
    public static void write(byte[] data, File file) {
        try {
            OutputStream os = new FileOutputStream(file);
            os.write(data);
            os.flush();
            os.close();
        } catch(IOException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Copy an stream to another one.
     */
    public static void copy(InputStream is, OutputStream os) {
        try {
            int read = 0;
            byte[] buffer = new byte[8096];
            while ((read = is.read(buffer)) > 0) {
                os.write(buffer, 0, read);
            }
            is.close();
        } catch(IOException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Copy an stream to another one.
     */
    public static void write(InputStream is, OutputStream os) {
        try {
            int read = 0;
            byte[] buffer = new byte[8096];
            while ((read = is.read(buffer)) > 0) {
                os.write(buffer, 0, read);
            }
            is.close();
            os.close();
        } catch(IOException e) {
            throw new UnexpectedException(e);
        }
    }

   /**
     * Copy an stream to another one.
     */
    public static void write(InputStream is, File f) {
        try {
            OutputStream os = new FileOutputStream(f);
            int read = 0;
            byte[] buffer = new byte[8096];
            while ((read = is.read(buffer)) > 0) {
                os.write(buffer, 0, read);
            }
            is.close();
            os.close();
        } catch(IOException e) {
            throw new UnexpectedException(e);
        }
    }
}
