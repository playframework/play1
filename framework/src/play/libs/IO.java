package play.libs;

import static java.nio.charset.Charset.defaultCharset;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.io.Charsets.toCharset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import play.exceptions.UnexpectedException;
import play.utils.OrderSafeProperties;

/**
 * IO utils
 */
public class IO {

    /**
     * Read a properties file with the utf-8 encoding
     * 
     * @param is
     *            Stream to properties file
     * @return The Properties object
     */
    public static Properties readUtf8Properties(InputStream is) {
        Properties properties = new OrderSafeProperties();
        try (is) {
            properties.load(is);
            return properties;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read the Stream content as a string (use utf-8)
     * 
     * @param is
     *            The stream to read
     * @return The String content
     */
    public static String readContentAsString(InputStream is) {
        return readContentAsString(is, "utf-8");
    }

    /**
     * Read the Stream content as a string
     * 
     * @param is
     *            The stream to read
     * @param encoding
     *            Encoding to used
     * @return The String content
     */
    public static String readContentAsString(InputStream is, String encoding) {
        try {
            return new String(is.readAllBytes(), toCharset(encoding));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read file content to a String (always use utf-8)
     * 
     * @param file
     *            The file to read
     * @return The String content
     */
    public static String readContentAsString(File file) {
        try {
            return Files.readString(file.toPath(), UTF_8);
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Read file content to a String
     * 
     * @param file
     *            The file to read
     * @param encoding
     *            Encoding to used
     * @return The String content
     */
    public static String readContentAsString(File file, String encoding) {
        try {
            return Files.readString(file.toPath(), toCharset(encoding));
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    public static List<String> readLines(InputStream is) {
        return new BufferedReader(new InputStreamReader(is, defaultCharset())).lines().collect(Collectors.toList());
    }

    public static List<String> readLines(File file, String encoding) {
        try {
            return Files.readAllLines(file.toPath(), toCharset(encoding));
        } catch (IOException ex) {
            throw new UnexpectedException(ex);
        }
    }

    public static List<String> readLines(File file) {
        try {
            return Files.readAllLines(file.toPath(), UTF_8);
        } catch (IOException ex) {
            throw new UnexpectedException(ex);
        }
    }

    /**
     * Read binary content of a file (warning does not use on large file !)
     * 
     * @param file
     *            The file te read
     * @return The binary data
     */
    public static byte[] readContent(File file) {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Read binary content of a stream (warning does not use on large file !)
     * 
     * @param is
     *            The stream to read
     * @return The binary data
     */
    public static byte[] readContent(InputStream is) {
        try {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Write String content to a stream (always use utf-8)
     * 
     * @param content
     *            The content to write
     * @param os
     *            The stream to write
     */
    public static void writeContent(CharSequence content, OutputStream os) {
        try (os) {
            if (content != null) {
                Channels.newChannel(os).write(UTF_8.encode(content.toString()));
            }
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Write String content to a stream (always use utf-8)
     * 
     * @param content
     *            The content to write
     * @param os
     *            The stream to write
     * @param encoding
     *            Encoding to used
     */
    public static void writeContent(CharSequence content, OutputStream os, String encoding) {
        try (os) {
            if (content != null) {
                Channels.newChannel(os).write(toCharset(encoding).encode(content.toString()));
            }
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Write String content to a file (always use utf-8)
     * 
     * @param content
     *            The content to write
     * @param file
     *            The file to write
     */
    public static void writeContent(CharSequence content, File file) {
        try {
            Files.writeString(file.toPath(), content, UTF_8);
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Write String content to a file (always use utf-8)
     * 
     * @param content
     *            The content to write
     * @param file
     *            The file to write
     * @param encoding
     *            Encoding to used
     */
    public static void writeContent(CharSequence content, File file, String encoding) {
        try {
            Files.writeString(file.toPath(), content, toCharset(encoding));
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Write binary data to a file
     * 
     * @param data
     *            The binary data to write
     * @param file
     *            The file to write
     */
    public static void write(byte[] data, File file) {
        try {
            Files.write(file.toPath(), data);
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Copy a stream to another one. By the end of the method the input stream {@code is} will be closed.
     * 
     * @param is
     *            The source stream
     * @param os
     *            The destination stream
     */
    public static void copy(InputStream is, OutputStream os) {
        try (is) {
            is.transferTo(os);
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Copy a stream to another one. By the end of the method both of streams {@code is} and {@code os} will be closed.
     * 
     * @param is
     *            The source stream
     * @param os
     *            The destination stream
     */
    public static void write(InputStream is, OutputStream os) {
        try (is; os) {
            is.transferTo(os);
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Copy an stream to another one.
     * 
     * @param is
     *            The source stream
     * @param f
     *            The destination file
     */
    public static void write(InputStream is, File f) {
        try (is; var os = new FileOutputStream(f)) {
            is.transferTo(os);
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    // If targetLocation does not exist, it will be created.
    public static void copyDirectory(File source, File target) {
        try (Stream<Path> dirs = Files.walk(source.toPath())) {
            dirs.forEach(src -> {
                try {
                    Files.copy(source.toPath(), target.toPath(), REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new UnexpectedException(e);
                }
            });
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

}
