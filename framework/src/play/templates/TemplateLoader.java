package play.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import play.Logger;
import play.Play;
import play.vfs.VirtualFile;
import play.exceptions.TemplateCompilationException;
import play.exceptions.TemplateNotFoundException;
import play.templates.loadingStrategies.*;

/**
 * Load templates
 */
public class TemplateLoader {

    private static TemplateLoadingStrategy loadingStrategy = null;

    protected static Map<String, BaseTemplate> templates = new HashMap<String, BaseTemplate>();
    /**
     * See getUniqueNumberForTemplateFile() for more info
     */
    private static AtomicLong nextUniqueNumber = new AtomicLong(1000);//we start on 1000
    private static Map<String, String> templateFile2UniqueNumber = new HashMap<String, String>();

    private static TemplateLoadingStrategy getTemplateLoadingStrategy() {
        if (loadingStrategy == null) {
            if (Play.usePrecompiled) {
                loadingStrategy = new LoadPrecompiledTemplates();
            } else {
                loadingStrategy = new LoadTemplatesFromSource();
            }
        }
        return loadingStrategy;
    }

    public static void addTemplatePath(VirtualFile path) {
        getTemplateLoadingStrategy().addTemplatePath(path);
    }

    /**
     * All loaded templates are cached in the templates-list using a key. This key is included as part of the classname for the
     * generated class for a specific template. The key is included in the classname to make it possible to resolve the original
     * template-file from the classname, when creating cleanStackTrace
     * 
     * This method returns a unique representation of the path which is usable as part of a classname
     * 
     * @param path
     * @return
     */
    public static String getUniqueNumberForTemplateFile(String path) {
        // a path cannot be a valid classname so we have to convert it somehow.
        // If we did some encoding on the path, the result would be at least as long as the path.
        // Therefore we assign a unique number to each path the first time we see it, and store it..
        // This way, all seen paths gets a unique number. This number is our UniqueValidClassnamePart..

        String uniqueNumber = templateFile2UniqueNumber.get(path);
        if (uniqueNumber == null) {
            //this is the first time we see this path - must assign a unique number to it.
            uniqueNumber = Long.toString(nextUniqueNumber.getAndIncrement());
            templateFile2UniqueNumber.put(path, uniqueNumber);
        }
        return uniqueNumber;
    }

    /**
     * Load a template from a virtual file
     * @param file A VirtualFile
     * @return The executable template
     */
    public static Template load(VirtualFile file) {
        Template pluginProvided = Play.pluginCollection.loadTemplate(file);
        if (pluginProvided != null) {
            return pluginProvided;
        }
        
        final String key = getUniqueNumberForTemplateFile(file.relativePath());

        BaseTemplate template = templates.get(key);
        if (template == null || getTemplateLoadingStrategy().needsReloading(template, file)) {
            template = getTemplateLoadingStrategy().load(file);

            if (template != null) {
                templates.put(key, template);
            } else {
                throw new TemplateNotFoundException(file.relativePath());
            }
        }
        return template;
    }

    private static String precompiledName(VirtualFile file) {
        return file.relativePath().replaceAll("\\{(.*)\\}", "from_$1").replace(":", "_").replace("..", "parent");
    }


    /**
     * Load template from a String, but don't cache it
     * @param source The template source
     * @return A Template
     */
    public static BaseTemplate loadString(String source) {
        BaseTemplate template = new GroovyTemplate(source);
        return new GroovyTemplateCompiler().compile(template);
    }

    /**
     * Cleans the cache for all templates
     */
    public static void cleanCompiledCache() {
        templates.clear();
    }

    /**
     * Cleans the specified key from the cache
     * @param key The template key
     */
    public static void cleanCompiledCache(String key) {
        templates.remove(key);
    }

    /**
     * Load a template
     * @param path The path of the template (ex: Application/index.html)
     * @return The executable template
     */
    public static Template load(String path) {
        VirtualFile resolvedTemplateFile = getTemplateLoadingStrategy().resolveTemplateName(path);

        if (resolvedTemplateFile != null) {
            return load(resolvedTemplateFile);
        } else {
            throw new TemplateNotFoundException(path);
        }
    }

    /**
     * List all found templates
     * @return A list of executable templates
     */
    public static List<Template> getAllTemplate() {
        List<Template> res = new ArrayList<Template>();
        for (VirtualFile virtualFile : getTemplateLoadingStrategy().getTemplatesPath()) {
            scan(res, virtualFile);
        }
        for (VirtualFile root : Play.roots) {
            VirtualFile vf = root.child("conf/routes");
            if (vf != null && vf.exists()) {
                Template template = load(vf);
                if (template != null) {
                    template.compile();
                }
            }
        }
        return res;
    }

    private static void scan(List<Template> templates, VirtualFile current) {
        if (!current.isDirectory() && !current.getName().startsWith(".") && !current.getName().endsWith(".scala.html")) {
            long start = System.currentTimeMillis();
            Template template = load(current);
            if (template != null) {
                try {
                    template.compile();
                    if (Logger.isTraceEnabled()) {
                        Logger.trace("%sms to load %s", System.currentTimeMillis() - start, current.getName());
                    }
                } catch (TemplateCompilationException e) {
                    Logger.error("Template %s does not compile at line %d", e.getTemplate().name, e.getLineNumber());
                    throw e;
                }
                templates.add(template);
            }
        } else if (current.isDirectory() && !current.getName().startsWith(".")) {
            for (VirtualFile virtualFile : current.list()) {
                scan(templates, virtualFile);
            }
        }
    }
}
