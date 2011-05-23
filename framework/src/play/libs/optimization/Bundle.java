package play.libs.optimization;

import java.io.*;
import play.Play;
import play.mvc.Http;

/**
 * A list of template/file sources for styles and scripts that can be compiled into one file (a "bundle").
 */
abstract public class Bundle
{
    /**
     * Where to save the bundle.
     */
    private final File bundleFile;

    /**
     * Where to save the bundle after GZIP.
     */
    private final File bundleFileGzip;

    /**
     * Filenames.
     */
    protected String[] sources;

    /**
     * "text/css" or "text/javascript"
     */
    private String mimeType;

    public Bundle(String filename, String[] sources, String mimeType)
    {
        this.bundleFile = Play.getFile("public/bundles/" + filename);
        this.bundleFileGzip = Play.getFile("public/bundles/" + filename + ".gzip");
        this.sources = sources;
        this.mimeType = mimeType;
    }

    abstract public void compile();

    public File getBundleFile()
    {
        return bundleFile;
    }

    public File getBundleFileGzip()
    {
        return bundleFileGzip;
    }

    /**
     * Filenames.
     */
    public String[] getSources()
    {
        return sources;
    }

    /**
     * @return "text/css" or "text/javascript"
     */
    public String getMimeType()
    {
        return mimeType;
    }

    /**
     * If the browser supports GZIP, return the GZIP file. If not supported, return the plain-text file.
     * Remember: you might want to add a caching header too.
     */
    public void applyToResponse(final Http.Request request, final Http.Response response)
    {
        // Only compile when absolutely needed (i.e. the files don't exist) because compiling is very slow.
        if (!getBundleFile().exists() || !getBundleFileGzip().exists())
        {
            compile();
        }

        // If the browser supports GZIP, return the GZIP file.
        final Http.Header acceptEncodingHeader = request.headers.get("accept-encoding"); // key must be lower-case.
        if (acceptEncodingHeader != null
            && acceptEncodingHeader.value().contains("gzip")
            && Boolean.parseBoolean(play.Play.configuration.getProperty("optimization.gzip", "false")))
        {
            response.setHeader("Content-Type", getMimeType());
            response.setHeader("Content-Encoding", "gzip");
            response.setHeader("Content-Length", getBundleFileGzip().length() + "");

            try
            {
                // renderBinary() will override any caching headers.
                response.direct = new FileInputStream(getBundleFileGzip());
            }
            catch (final FileNotFoundException ex)
            {
                throw new RuntimeException(ex);
            }
        }
        // GZIP not supported by the browser.
        else
        {
            response.setHeader("Content-Type", getMimeType());
            response.setHeader("Content-Length", getBundleFile().length() + "");

            try
            {
                // renderBinary() will override any caching headers.
                response.direct = new FileInputStream(getBundleFile());
            }
            catch (final FileNotFoundException ex)
            {
                throw new RuntimeException(ex);
            }
        }
    }
}
