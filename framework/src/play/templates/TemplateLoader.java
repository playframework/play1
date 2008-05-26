package play.templates;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TemplateLoader {
    
    static Map<File,Template> templates = new HashMap<File, Template>();
    
    public static Template load(File file) {
        if(!templates.containsKey(file)) {
            templates.put(file, TemplateCompiler.compile(file));
        }
        return templates.get(file);
    }

}
