package play.templates;

import groovy.lang.Closure;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import play.data.validation.Error;
import play.data.validation.Validation;
import play.exceptions.TagInternalException;
import play.exceptions.TemplateExecutionException;
import play.exceptions.TemplateNotFoundException;
import play.libs.Codec;
import play.mvc.Router.ActionDefinition;
import play.templates.Template.ExecutableTemplate;

/**
 * Fast tags implementation
 */
public class FastTags {

    public static void _verbatim(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        out.println(JavaExtensions.toString(body));
    }

    public static void _jsAction(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        out.println("function(options) {var pattern = '" + args.get("arg").toString() + "'; for(key in options) { pattern = pattern.replace(':'+key, options[key]); } return pattern };");
    }

    public static void _select(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        String name = args.get("arg").toString();
        Object value = args.get("value");
        TagContext.current().data.put("selected", value);
        out.print("<select name=\"" + name +"\" size=\"1\">");
        out.println(JavaExtensions.toString(body));
        out.print("</select>");
    }

    public static void _option(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        Object value = args.get("arg");
        Object selectedValue = TagContext.parent("select").data.get("selected");
        boolean selected = selectedValue != null && value != null && selectedValue.equals(value);
        out.print("<option value=\""+value+"\" "+(selected ? "selected" : "")+">");
        out.println(JavaExtensions.toString(body));
        out.print("</option>");
    }


    /**
     * Generates a html form element linked to a controller action
     * @param args tag attributes
     * @param body tag inner body
     * @param out the output writer
     * @param template enclosing template
     * @param fromLine template line number where the tag is defined
     */
    public static void _form(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        ActionDefinition actionDef = (ActionDefinition) args.get("arg");
        if (actionDef == null) {
            actionDef = (ActionDefinition) args.get("action");
        }
        String enctype = (String) args.get("enctype");
        if (enctype == null) {
            enctype = "application/x-www-form-urlencoded";
        }
        if (actionDef.star) {
            actionDef.method = "POST"; // prefer POST for form ....
        }
        if (args.containsKey("method")) {
            actionDef.method = args.get("method").toString();
        }
        if (!("GET".equals(actionDef.method) || "POST".equals(actionDef.method))) {
            String separator = actionDef.url.indexOf('?') != -1 ? "&" : "?";
            actionDef.url += separator + "x-http-method-override=" + actionDef.method;
            actionDef.method = "POST";
        }
        out.print("<form " + (args.get("id") == null ? "" : "id=\"" + args.get("id") + "\" ") + "action=\"" + actionDef.url + "\" method=\"" + actionDef.method + "\" accept-charset=\"utf-8\" enctype=\"" + enctype + "\">");
        out.println(JavaExtensions.toString(body));
        out.print("</form>");
    }

    /**
     * Generates a html link to a controller action
     * @param args tag attributes
     * @param body tag inner body
     * @param out the output writer
     * @param template enclosing template
     * @param fromLine template line number where the tag is defined
     */
    public static void _a(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        ActionDefinition actionDef = (ActionDefinition) args.get("arg");
        if (actionDef == null) {
            actionDef = (ActionDefinition) args.get("action");
        }
        if (!("GET".equals(actionDef.method))) {
            if (!("POST".equals(actionDef.method))) {
                String separator = actionDef.url.indexOf('?') != -1 ? "&" : "?";
                actionDef.url += separator + "x-http-method-override=" + actionDef.method;
                actionDef.method = "POST";
            }
            String id = Codec.UUID();
            out.print("<form method=\"POST\" id=\"" + id + "\" style=\"display:none\" action=\"" + actionDef.url + "\"></form>");
            out.print("<a" + (args.get("id") == null ? "" : " id=\"" + args.get("id") + "\" ") + " href=\"javascript:document.getElementById('" + id + "').submit();\">");
            out.print(JavaExtensions.toString(body));
            out.print("</a>");
        } else {
            out.print("<a" + (args.get("id") == null ? "" : " id=\"" + args.get("id") + "\" ") + " href=\"" + actionDef.url + "\">");
            out.print(JavaExtensions.toString(body));
            out.print("</a>");
        }
    }

    public static void _ifErrors(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        if (Validation.hasErrors()) {
            body.call();
            TagContext.parent().data.put("_executeNextElse", false);
        } else {
            TagContext.parent().data.put("_executeNextElse", true);
        }
    }

    public static void _ifError(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        if (args.get("arg") == null) {
            throw new TemplateExecutionException(template.template, fromLine, "Please specify the error key", new TagInternalException("Please specify the error key"));
        }
        if (Validation.hasError(args.get("arg").toString())) {
            body.call();
            TagContext.parent().data.put("_executeNextElse", false);
        } else {
            TagContext.parent().data.put("_executeNextElse", true);
        }
    }

    public static void _errorClass(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        if (args.get("arg") == null) {
            throw new TemplateExecutionException(template.template, fromLine, "Please specify the error key", new TagInternalException("Please specify the error key"));
        }
        if (Validation.hasError(args.get("arg").toString())) {
            out.print("hasError");
        }
    }

    public static void _error(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        if (args.get("arg") == null && args.get("key") == null) {
            throw new TemplateExecutionException(template.template, fromLine, "Please specify the error key", new TagInternalException("Please specify the error key"));
        }
        String key = args.get("arg") == null ? args.get("key") + "" : args.get("arg") + "";
        Error error = Validation.error(key);
        if (error != null) {
            if (args.get("field") == null) {
                out.print(error.message());
            } else {
                out.print(error.message(args.get("field") + ""));
            }
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

    public static void _doLayout(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        out.print("____%LAYOUT%____");
    }

    public static void _get(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        Object name = args.get("arg");
        if (name == null) {
            throw new TemplateExecutionException(template.template, fromLine, "Specify a variable name", new TagInternalException("Specify a variable name"));
        }
        Object value = Template.layoutData.get().get(name);
        if (value != null) {
            out.print(value);
        } else {
            if (body != null) {
                out.print(JavaExtensions.toString(body));
            }
        }
    }

    public static void _set(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        // Simple case : #{set title:'Yop' /}
        for (Map.Entry<?, ?> entry : args.entrySet()) {
            Object key = entry.getKey();
            if (!key.toString().equals("arg")) {
                Template.layoutData.get().put(key, entry.getValue());
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

    public static void _extends(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        try {
            if (!args.containsKey("arg") || args.get("arg") == null) {
                throw new TemplateExecutionException(template.template, fromLine, "Specify a template name", new TagInternalException("Specify a template name"));
            }
            String name = args.get("arg").toString();
            if (name.startsWith("./")) {
                String ct = Template.currentTemplate.get().name;
                if (ct.matches("^/lib/[^/]+/app/views/.*")) {
                    ct = ct.substring(ct.indexOf("/", 5));
                }
                ct = ct.substring(0, ct.lastIndexOf("/"));
                name = ct + name.substring(1);
            }
            Template.layout.set(TemplateLoader.load(name));
        } catch (TemplateNotFoundException e) {
            throw new TemplateNotFoundException(e.getPath(), template.template, fromLine);
        }
    }

    public static void _include(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        try {
            if (!args.containsKey("arg") || args.get("arg") == null) {
                throw new TemplateExecutionException(template.template, fromLine, "Specify a template name", new TagInternalException("Specify a template name"));
            }
            String name = args.get("arg").toString();
            if (name.startsWith("./")) {
                String ct = Template.currentTemplate.get().name;
                if (ct.matches("^/lib/[^/]+/app/views/.*")) {
                    ct = ct.substring(ct.indexOf("/", 5));
                }
                ct = ct.substring(0, ct.lastIndexOf("/"));
                name = ct + name.substring(1);
            }
            Template t = TemplateLoader.load(name);
            Map<String, Object> newArgs = new HashMap<String, Object>();
            newArgs.putAll(template.getBinding().getVariables());
            newArgs.put("_isInclude", true);
            t.render(newArgs);
        } catch (TemplateNotFoundException e) {
            throw new TemplateNotFoundException(e.getPath(), template.template, fromLine);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public static @interface Namespace {

        String value() default "";

    }

}
