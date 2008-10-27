package play.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.Logger;
import play.Play;
import play.vfs.VirtualFile;
import play.exceptions.TemplateCompilationException;
import play.exceptions.TemplateNotFoundException;

public class TemplateLoader {

    static Map<VirtualFile, Template> templates = new HashMap<VirtualFile, Template>();

    static Template load(VirtualFile file) {
        if (!templates.containsKey(file)) {
            templates.put(file, TemplateCompiler.compile(file));
        }
        Template template = templates.get(file);
        if (Play.mode == Play.Mode.DEV && template.timestamp < file.lastModified()) {
            templates.put(file, TemplateCompiler.compile(file));
        }
        if (templates.get(file) == null) {
            throw new TemplateNotFoundException(file.relativePath());
        }
        return templates.get(file);
    }

    public static void cleanCompiledCache() {
        for (Template template : templates.values()) {
            if (template.needJavaRecompilation) {
                template.compiledTemplate = null;
            }
        }
    }

    public static Template load(String path) {
        Template template = null;
        for (VirtualFile vf : Play.templatesPath) {
            VirtualFile tf = vf.child(path);
            if (tf.exists()) {
                template = TemplateLoader.load(tf);
                break;
            }
        }
        if (template == null) {
            VirtualFile tf = Play.getVirtualFile(path);
            if (tf.exists()) {
                template = TemplateLoader.load(tf);
            } else {
                throw new TemplateNotFoundException(path);
            }
        }
        return template;
    }

    public static List<Template> getAllTemplate() {
        List<Template> res = new ArrayList<Template>();
        for (VirtualFile virtualFile : Play.templatesPath) {
            scan(res, virtualFile);
        }
        return res;
    }

    private static void scan(List<Template> templates, VirtualFile current) {
        if (!current.isDirectory()) {
            long start = System.currentTimeMillis();
            Template template = load(current);
            try {
                template.compile();
                Logger.trace("%sms to load %s", System.currentTimeMillis()-start, current.getName());
            } catch (TemplateCompilationException e) {
                Logger.error("Template %s does not compile at line %d", e.getTemplate().name, e.getLineNumber());
                throw e;
            }
            templates.add(template);
        } else {
            for (VirtualFile virtualFile : current.list()) {
                scan(templates, virtualFile);
            }
        }
    }
}
