package play.libs.optimization;

import java.util.zip.*;
import java.io.*;

/**
 * @see http://code.google.com/p/htmlcompressor/
 * @see http://developer.yahoo.com/yui/compressor/
 */
public class Compression
{
    /**
     * Compresses HTML by removing whitespace, etc.
     */
    // Uses http://code.google.com/p/htmlcompressor/
    public static String compressHTML(final String input)
    {
        try
        {
            return new com.googlecode.htmlcompressor.compressor.HtmlCompressor().compress(input);
        }
        catch (Exception ex)
        {
            play.Logger.error(ex, "HTML compression error: %s", ex.getMessage());
            return input;
        }
    }

    /**
     * Compresses XML by removing whitespace, etc.
     */
    // Uses http://code.google.com/p/htmlcompressor/
    public static String compressXML(final String input)
    {
        try
        {
            return new com.googlecode.htmlcompressor.compressor.XmlCompressor().compress(input);
        }
        catch (Exception ex)
        {
            play.Logger.error(ex, "XML compression error: %s", ex.getMessage());
            return input;
        }
    }

    /**
     * Compresses CSS by removing whitespace, etc.
     */
    // YUI compressor is used because it has no dependencies and is very lightweight.
    public static String compressCSS(final String input)
    {
        try
        {
            return new com.googlecode.htmlcompressor.compressor.YuiCssCompressor().compress(input);
        }
        catch (Exception ex)
        {
            play.Logger.error(ex, "CSS compression error: %s", ex.getMessage());
            return input;
        }
    }

    /**
     * GZIP "input".
     */
    public static ByteArrayOutputStream gzip(final String input)
        throws IOException
    {
        final InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        final ByteArrayOutputStream stringOutputStream = new ByteArrayOutputStream((int) (input.length() * 0.75));
        final OutputStream gzipOutputStream = new GZIPOutputStream(stringOutputStream);

        final byte[] buf = new byte[5000];
        int len;
        while ((len = inputStream.read(buf)) > 0)
        {
            gzipOutputStream.write(buf, 0, len);
        }

        inputStream.close();
        gzipOutputStream.close();

        return stringOutputStream;
    }
}
