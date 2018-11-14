package tags;

import groovy.lang.Closure;
import play.templates.FastTags;
import play.templates.GroovyTemplate.ExecutableTemplate;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;


@FastTags.Namespace
public class OverridenFastTag extends FastTags {

    public static void _field(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        Map<String,Object> field = new HashMap<String,Object>();
        field.put("id", "overriden");
        body.setProperty("field", field);
        body.call();
    }
}
