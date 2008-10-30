package play.templates;

import groovy.lang.Closure;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import play.exceptions.TagInternalException;
import play.exceptions.TemplateExecutionException;
import play.exceptions.TemplateNotFoundException;
import play.templates.Template.ExecutableTemplate;

public class FastTags {

    public static void _if(Map args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        if (_evaluateCondition(args.get("arg"))) {
            body.call();
            TagContext.parent().data.put("_executeNextElse", false);
        } else {
            TagContext.parent().data.put("_executeNextElse", true);
        }
    }
    
    public static void _else(Map args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        if(TagContext.parent().data.containsKey("_executeNextElse")) {
            if((Boolean)TagContext.parent().data.get("_executeNextElse")) {
                body.call();                
            }
            TagContext.parent().data.remove("_executeNextElse");
        } else {
            throw new TemplateExecutionException(template.template, fromLine, "else tag without a preceding if tag", new TagInternalException("else tag without a preceding if tag"));
        }
    }
    
    public static void _elseif(Map args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        if(TagContext.parent().data.containsKey("_executeNextElse")) {
            if((Boolean)TagContext.parent().data.get("_executeNextElse") && _evaluateCondition(args.get("arg"))) {
                body.call(); 
                TagContext.parent().data.put("_executeNextElse", false);
            }            
        } else {
            throw new TemplateExecutionException(template.template, fromLine, "elseif tag without a preceding if tag", new TagInternalException("elseif tag without a preceding if tag"));
        }
    }
        
    public static void _ifnot(Map args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        if (!_evaluateCondition(args.get("arg"))) {
            body.call();
            TagContext.parent().data.put("_executeNextElse", false);
        } else {
            TagContext.parent().data.put("_executeNextElse", true);
        }
    }
    
    static boolean _evaluateCondition(Object test) {
        if (test != null) {
            if (test instanceof Boolean) {
                return ((Boolean) test).booleanValue();
            } else if (test instanceof String) {
                return ((String) test).length() > 0;
            } else if (test instanceof Number) {
                return ((Number) test).intValue() != 0;
            } else if (test instanceof Collection) {
                return ((Collection) test).size() != 0;
            } else {
                return true;
            }
        }
        return false;
    }

    public static void _doLayout(Map args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        out.print("____%LAYOUT%____");
    }

    public static void _get(Map args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        Object name = args.get("arg");
        if(name == null) {
            throw new TemplateExecutionException(template.template, fromLine, "Specify a variable name", new TagInternalException("Specify a variable name"));
        }
        Object value = Template.layoutData.get().get(name);
        if(value != null) {
            out.print(Template.layoutData.get().get(name));
        }
    }

    public static void _set(Map args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        // Simple case : #{set title:'Yop' /}
        for (Object p : args.keySet()) {
            if (!p.toString().equals("arg")) {
                Template.layoutData.get().put(p.toString(), args.get(p));
                return;
            }
        }
        // Body case
        Object name = args.get("arg");
        if (name != null && body != null) {
            Object oldOut = body.getProperty("out");
            StringWriter sw = new StringWriter();
            body.setProperty("out", new PrintWriter(sw));
            body.call();
            Template.layoutData.get().put(name, sw.toString());
            body.setProperty("out", oldOut);
        }
    }

    public static void _extends(Map args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        try {
            if(!args.containsKey("arg") || args.get("arg") == null) {
                throw new TemplateExecutionException(template.template, fromLine, "Specify a template name", new TagInternalException("Specify a template name"));
            }
            String name = args.get("arg").toString();
            if (name.startsWith("./")) {
                String ct = Template.currentTemplate.get().name;
                ct = ct.substring(0, ct.lastIndexOf("/"));
                name = ct + name.substring(1);
            }
            Template.layout.set(TemplateLoader.load(name));
        } catch(TemplateNotFoundException e) {
            throw new TemplateNotFoundException(e.getPath(), template.template, fromLine);
        }
    }
    
    public static void _include(Map args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        try {
            if(!args.containsKey("arg") || args.get("arg") == null) {
                throw new TemplateExecutionException(template.template, fromLine, "Specify a template name", new TagInternalException("Specify a template name"));
            }
            String name = args.get("arg").toString();
            Template t = TemplateLoader.load(name);
            Map newArgs = new HashMap();
            newArgs.putAll(template.getBinding().getVariables());
            newArgs.put("_isInclude", true);
            t.render(newArgs);
        } catch(TemplateNotFoundException e) {
            throw new TemplateNotFoundException(e.getPath(), template.template, fromLine);
        }
    }
}
