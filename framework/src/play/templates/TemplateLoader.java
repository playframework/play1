package play.templates;

import java.util.HashMap;
import java.util.Map;
import play.Play.VirtualFile;
import play.exceptions.TemplateNotFoundException;

public class TemplateLoader {
    
    static Map<VirtualFile,Template> templates = new HashMap<VirtualFile, Template>();
    
    public static Template load(VirtualFile file) {
        if(!templates.containsKey(file)) {
            templates.put(file, TemplateCompiler.compile(file));
        }
        Template template = templates.get(file);
        if(template.timestamp<file.lastModified()) {
            templates.put(file, TemplateCompiler.compile(file));
        }
        if(templates.get(file) == null) {
            throw new TemplateNotFoundException(file.relativePath());
        }
        return templates.get(file);
    }
    
    public static void cleanCompiledCache() {
        for(Template template : templates.values()) {
            template.compiledTemplate = null;
        }
    }

}
