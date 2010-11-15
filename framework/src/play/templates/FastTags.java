package play.templates;

import groovy.lang.Closure;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import play.cache.Cache;
import play.data.validation.Error;
import play.data.validation.Validation;
import play.exceptions.TagInternalException;
import play.exceptions.TemplateExecutionException;
import play.exceptions.TemplateNotFoundException;
import play.libs.Codec;
import play.mvc.Router.ActionDefinition;
import play.mvc.Scope.Flash;
import play.mvc.Scope.Session;
import play.templates.GroovyTemplate.ExecutableTemplate;

/**
 * Fast tags implementation
 */
public class FastTags {

    public static void _cache(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        String key = args.get("arg").toString();
        String duration = null;
        if(args.containsKey("for")) {
            duration = args.get("for").toString();
        }
        Object cached = Cache.get(key);
        if(cached != null) {
            out.print(cached);
            return;
        }
        String result = JavaExtensions.toString(body);
        Cache.set(key, result, duration);
        out.print(result);
    }

    public static void _verbatim(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        out.println(JavaExtensions.toString(body));
    }

    public static void _jsAction(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        out.println("function(options) {var pattern = '" + args.get("arg").toString().replace("&amp;", "&") + "'; for(key in options) { pattern = pattern.replace(':'+key, options[key]); } return pattern };");
    }

    public static void _authenticityToken(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        out.println("<input type=\"hidden\" name=\"authenticityToken\" value=\"" + Session.current().getAuthenticityToken() + "\">");
    }

    public static void _option(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        Object value = args.get("arg");
        Object selectedValue = TagContext.parent("select").data.get("selected");
        boolean selected = selectedValue != null && value != null && selectedValue.equals(value);
        out.print("<option value=\"" + (value == null ? "" : value) + "\" " + (selected ? "selected=\"selected\"" : "") + "" + serialize(args, "selected", "value") + ">");
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
            actionDef.url += separator + "x-http-method-override=" + actionDef.method.toUpperCase();
            actionDef.method = "POST";
        }
        out.print("<form action=\"" + actionDef.url + "\" method=\"" + actionDef.method.toUpperCase() + "\" accept-charset=\"utf-8\" enctype=\"" + enctype + "\" " + serialize(args, "action", "method", "accept-charset", "enctype") + ">");
        if (!("GET".equals(actionDef.method))) {
            _authenticityToken(args, body, out, template, fromLine);
        }
        out.println(JavaExtensions.toString(body));
        out.print("</form>");
    }
    
    public static void _field(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        Map<String,Object> field = new HashMap<String,Object>();
        String _arg = args.get("arg").toString();
        field.put("name", _arg);
        field.put("id", _arg.replace('.','_'));
        field.put("flash", Flash.current().get(_arg));
        field.put("flashArray", field.get("flash") != null && !field.get("flash").toString().isEmpty() ? field.get("flash").toString().split(",") : new String[0]);
        field.put("error", Validation.error(_arg));
        field.put("errorClass", field.get("error") != null ? "hasError" : "");
        String[] c = _arg.split("\\.");
        Object obj = body.getProperty(c[0]);
        if(obj != null){
            try{
                Field f = obj.getClass().getField(c[1]);
                field.put("value", f.get(obj).toString());
            }catch(Exception e){
            }
        }
        body.setProperty("field", field);
        body.call();
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
            out.print("<form method=\"POST\" id=\"" + id + "\" style=\"display:none\" action=\"" + actionDef.url + "\">");
            _authenticityToken(args, body, out, template, fromLine);
            out.print("</form>");
            out.print("<a href=\"javascript:document.getElementById('" + id + "').submit();\" " + serialize(args, "href") + ">");
            out.print(JavaExtensions.toString(body));
            out.print("</a>");
        } else {
            out.print("<a href=\"" + actionDef.url + "\" " + serialize(args, "href") + ">");
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
        Object value = BaseTemplate.layoutData.get().get(name);
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
                BaseTemplate.layoutData.get().put(key, entry.getValue());
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
            BaseTemplate.layoutData.get().put(name, sw.toString());
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
                String ct = BaseTemplate.currentTemplate.get().name;
                if (ct.matches("^/lib/[^/]+/app/views/.*")) {
                    ct = ct.substring(ct.indexOf("/", 5));
                }
                ct = ct.substring(0, ct.lastIndexOf("/"));
                name = ct + name.substring(1);
            }
            BaseTemplate.layout.set((BaseTemplate)TemplateLoader.load(name));
        } catch (TemplateNotFoundException e) {
            throw new TemplateNotFoundException(e.getPath(), template.template, fromLine);
        }
    }

    @SuppressWarnings("unchecked")
    public static void _include(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        try {
            if (!args.containsKey("arg") || args.get("arg") == null) {
                throw new TemplateExecutionException(template.template, fromLine, "Specify a template name", new TagInternalException("Specify a template name"));
            }
            String name = args.get("arg").toString();
            if (name.startsWith("./")) {
                String ct = BaseTemplate.currentTemplate.get().name;
                if (ct.matches("^/lib/[^/]+/app/views/.*")) {
                    ct = ct.substring(ct.indexOf("/", 5));
                }
                ct = ct.substring(0, ct.lastIndexOf("/"));
                name = ct + name.substring(1);
            }
            BaseTemplate t = (BaseTemplate)TemplateLoader.load(name);
            Map<String, Object> newArgs = new HashMap<String, Object>();
            newArgs.putAll(template.getBinding().getVariables());
            newArgs.put("_isInclude", true);
            t.render(newArgs);
        } catch (TemplateNotFoundException e) {
            throw new TemplateNotFoundException(e.getPath(), template.template, fromLine);
        }
    }

    @SuppressWarnings("unchecked")
    public static void _render(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        try {
            if (!args.containsKey("arg") || args.get("arg") == null) {
                throw new TemplateExecutionException(template.template, fromLine, "Specify a template name", new TagInternalException("Specify a template name"));
            }
            String name = args.get("arg").toString();
            if (name.startsWith("./")) {
                String ct = BaseTemplate.currentTemplate.get().name;
                if (ct.matches("^/lib/[^/]+/app/views/.*")) {
                    ct = ct.substring(ct.indexOf("/", 5));
                }
                ct = ct.substring(0, ct.lastIndexOf("/"));
                name = ct + name.substring(1);
            }
            args.remove("arg");
            BaseTemplate t = (BaseTemplate)TemplateLoader.load(name);
            Map<String, Object> newArgs = new HashMap<String, Object>();
            newArgs.putAll((Map<? extends String, ? extends Object>) args);
            newArgs.put("_isInclude", true);
            out.println(t.render(newArgs));
        } catch (TemplateNotFoundException e) {
            throw new TemplateNotFoundException(e.getPath(), template.template, fromLine);
        }
    }

    public static String serialize(Map<?, ?> args, String... unless) {
        StringBuffer attrs = new StringBuffer();
        Arrays.sort(unless);
        for (Object o : args.keySet()) {
            String attr = o.toString();
            String value = args.get(o) == null ? "" : args.get(o).toString();
            if (Arrays.binarySearch(unless, attr) < 0 && !attr.equals("arg")) {
                attrs.append(attr);
                attrs.append("=\"");
                attrs.append(value);
                attrs.append("\" ");
            }
        }
        return attrs.toString();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public static @interface Namespace {

        String value() default "";
    }
}
