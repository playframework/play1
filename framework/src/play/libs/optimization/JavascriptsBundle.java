package play.libs.optimization;

import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSSourceFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.lang.ArrayUtils;
import play.Logger;
import play.Play;
import play.libs.IO;
import play.templates.TemplateLoader;

/**
 * A list of template/file sources for scripts that can be compiled into one file (a "bundle").
 * Sources are the "interns" in Google Closure Compiler.
 */
public class JavascriptsBundle extends Bundle
{
    /**
     * If null, compiling is disabled.
     */
    private CompilationLevel compilationLevel;

    /**
     * Default externs (e.g. HTML DOM properties) are automatically added to this.
     */
    private String[] externs;

    /**
     * Compilation level default is {@link CompilationLevel.SIMPLE_OPTIMIZATIONS}.
     * Interns must be set by {@link #setSources}.
     *
     * @param filename May be anything. Not used publicly.
     */
    public JavascriptsBundle(String filename)
    {
        super(filename, new String[] { }, "application/javascript");
        this.compilationLevel = CompilationLevel.SIMPLE_OPTIMIZATIONS;
    }

    /**
     * Compilation level default is {@link CompilationLevel.SIMPLE_OPTIMIZATIONS}.
     *
     * @param filename May be anything. Not used publicly.
     * @param sources Filenames.
     */
    public JavascriptsBundle(String filename, String... sources)
    {
        super(filename, sources, "application/javascript");
        this.compilationLevel = CompilationLevel.SIMPLE_OPTIMIZATIONS;
    }

    /**
     * @param filename May be anything. Not used publicly.
     * @param compilationLevel If null, compiling is disabled.
     * @param sources Filenames.
     */
    public JavascriptsBundle(String filename, CompilationLevel compilationLevel, String... sources)
    {
        super(filename, sources, "application/javascript");
        this.compilationLevel = compilationLevel;
    }

    public JavascriptsBundle setInterns(String... sources)
    {
        this.sources = sources;
        return this;
    }

    /**
     * @param externs Default externs (e.g. DOM properties) are automatically added to this.
     * @return Same object.
     */
    public JavascriptsBundle setExterns(String... externs)
    {
        this.externs = externs;
        return this;
    }

    /**
     * @param compilationLevel If null, compiling is disabled.
     * @return Same object.
     */
    public JavascriptsBundle setCompilationLevel(CompilationLevel compilationLevel)
    {
        this.compilationLevel = compilationLevel;
        return this;
    }

    /**
     * Compile Javascript files/sources with the Google Closure Compiler.
     */
    @Override
    public void compile()
    {
        Logger.info("Compiling JS bundle: %s", ArrayUtils.toString(sources));

        Compiler.setLoggingLevel(Level.SEVERE);

        if (compilationLevel != null)
        {
            compileFiles(compilationLevel, getInternsAsJSSourceFiles(), getExternsAsJSSourceFiles());
        }
        else
        {
            concatenateFiles(getInternsAsJSSourceFiles());
        }
    }

    /**
     * @param compilationLevel Must not be null.
     * @param interns
     * @param externs
     */
    private void compileFiles(CompilationLevel compilationLevel, List<JSSourceFile> interns, List<JSSourceFile> externs)
    {
        final Compiler compiler = new Compiler();
        final CompilerOptions options = new CompilerOptions();
        compilationLevel.setOptionsForCompilationLevel(options);

        // compile() returns a Result, but it is not needed here.
        compiler.compile(externs, interns, options);

        // Save to file and GZIP.
        IO.writeContent(compiler.toSource(), getBundleFile());

        Compression.gzip(getBundleFile(), getBundleFileGzip());
    }

    /**
     * Concatenate the original code files.
     */
    private void concatenateFiles(List<JSSourceFile> jsSourceFiles)
    {
        try
        {
            StringBuilder uncompiledJS = new StringBuilder(50000);
            for (JSSourceFile jsSourceFile : jsSourceFiles)
            {
                uncompiledJS.append(jsSourceFile.getCode());
            }

            // Save to file and GZIP.
            IO.writeContent(uncompiledJS.toString(), getBundleFile());
            Compression.gzip(getBundleFile(), getBundleFileGzip());

            uncompiledJS = null;
        }
        catch (IOException ex)
        {
            play.Logger.error(ex, "Concatenating JS error");
        }
    }

    private List<JSSourceFile> getInternsAsJSSourceFiles()
    {
        final List<JSSourceFile> interns = new ArrayList<JSSourceFile>(4);

        // Add some licensing and debugging info.
        interns.add(JSSourceFile.fromCode("copyright",
            String.format("/** @license Copyright 2011 %s */", Play.configuration.getProperty("application.name", "App Name Here"))));
        interns.add(JSSourceFile.fromCode("preserve1",
            String.format("/** @preserve Compiled %s using level %s */", System.currentTimeMillis(), compilationLevel == null
            ? "OFF" : compilationLevel.name())));
        interns.add(JSSourceFile.fromCode("preserve2",
            String.format("/** @preserve Interns: %s */", ArrayUtils.toString(sources))));
        interns.add(JSSourceFile.fromCode("preserve3",
            String.format("/** @preserve Externs: %s */", ArrayUtils.toString(externs))));

        addJSSourceFilesFromTemplatePaths(interns, sources);

        return interns;
    }

    private List<JSSourceFile> getExternsAsJSSourceFiles()
    {
        List<JSSourceFile> externs = new ArrayList<JSSourceFile>(4);
        try
        {
            externs = CommandLineRunner.getDefaultExterns();
        }
        catch (IOException ex)
        {
            play.Logger.error(ex, "Could not get default externs");
            // Try to compile anyway, even though it will probably output code that doesn't work.
            externs = new ArrayList<JSSourceFile>(0);
        }
        addJSSourceFilesFromTemplatePaths(externs, this.externs);

        return externs;
    }

    private static void addJSSourceFilesFromTemplatePaths(List<JSSourceFile> jsSourceFiles, String[] templates)
    {
        if (templates == null)
        {
            return;
        }
        for (final String t : templates)
        {
            jsSourceFiles.add(JSSourceFile.fromCode(t, TemplateLoader.load(t).render()));
        }
    }
}
