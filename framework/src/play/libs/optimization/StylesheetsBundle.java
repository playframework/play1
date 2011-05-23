package play.libs.optimization;

import org.apache.commons.lang.ArrayUtils;
import play.Logger;
import play.Play;
import play.exceptions.UnexpectedException;
import play.libs.IO;
import play.templates.TemplateLoader;

/**
 * A bundle of compiled CSS stylesheets.

 * Refer to <a href="http://developer.yahoo.com/yui/compressor/css.html">the YUI docs</a> for more info.
 */
public class StylesheetsBundle extends Bundle
{
    /**
     * @param filename May be anything. Not used publicly.
     * @param sources Filenames.
     */
    public StylesheetsBundle(String filename, String... sources)
    {
        super(filename, sources, "text/css");
    }

    /**
     * Compile CSS files/sources with the Yahoo YUI Compressor.
     */
    @Override
    public void compile()
    {
        Logger.info("Compiling CSS bundle: %s", ArrayUtils.toString(getSources()));

        getBundleFile().delete();
        getBundleFileGzip().delete();

        final StringBuilder fullUncompiledCSS = new StringBuilder(15000);
        addLicense(fullUncompiledCSS);
        try
        {
            for (final String source : getSources())
            {
                fullUncompiledCSS.append(TemplateLoader.load(Play.getVirtualFile(source)).render());
            }

            IO.writeContent(Compression.compressCSS(fullUncompiledCSS.toString()), getBundleFile());
        }
        catch (final UnexpectedException ex)
        {
            Logger.error(ex, "Compile CSS error: " + ex.getMessage());
            try
            {
                // As a fall-back, use the uncompiled CSS.
                IO.writeContent(fullUncompiledCSS.toString(), getBundleFile());
            }
            catch (final UnexpectedException ex2)
            {
                Logger.error(ex2, "Compile CSS error: " + ex2.getMessage());
            }
        }

        Compression.gzip(getBundleFile(), getBundleFileGzip());
    }

    /**
     * Adds a license that will not be removed by the YUI compressor.
     */
    private static void addLicense(StringBuilder fullUncompiledCSS)
    {
        String appName = Play.configuration.getProperty("application.name");
        String license = String.format("/*! Copyright 2011 %s, compiled %s */", appName, System.currentTimeMillis());

        fullUncompiledCSS.append(license);
    }
}
