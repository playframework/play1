package play.mvc;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Document;
import play.Play;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.data.binding.Unbinder;
import play.exceptions.NoRouteFoundException;
import play.exceptions.PlayException;
import play.exceptions.TemplateNotFoundException;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Response;
import play.mvc.results.Error;
import play.mvc.results.Forbidden;
import play.mvc.results.NotFound;
import play.mvc.results.Redirect;
import play.mvc.results.RenderBinary;
import play.mvc.results.RenderTemplate;
import play.mvc.results.RenderText;
import play.mvc.results.RenderJson;
import play.mvc.results.RenderXml;
import play.mvc.results.Unauthorized;
import play.templates.Template;
import play.templates.TemplateLoader;

/**
 * Application controller support
 */
public abstract class Controller {

    /**
     * The current HTTP request
     */
    public static Http.Request request = null;
    /**
     * The current HTTP response
     */
    public static Response response = null;
    /**
     * The current HTTP session
     */
    public static Scope.Session session = null;
    /**
     * The current flash scope
     */
    public static Scope.Flash flash = null;
    /**
     * The current HTTP params
     */
    public static Scope.Params params = null;
    /**
     * The current renderArgs (used in templates)
     */
    public static Scope.RenderArgs renderArgs = null;

    /**
     * Return a 200 OK text/plain response
     * @param text The response content
     */
    protected static void renderText(Object text) {
        throw new RenderText(text == null ? "" : text.toString());
    }

    /**
     * Return a 200 OK text/plain response
     * @param text The response content to be formatted (with String.format)
     * @param args Args for String.format
     */
    protected static void renderText(CharSequence pattern, Object... args) {
        throw new RenderText(String.format(pattern.toString(), args));
    }

    /**
     * Return a 200 OK text/xml response
     * @param xml The XML string
     */
    protected static void renderXml(String xml) {
        throw new RenderXml(xml);
    }
    
    /**
     * Return a 200 OK text/xml response
     * @param xml The DOM document object
     */
    protected static void renderXml(Document xml) {
        throw new RenderXml(xml);
    }

    /**
     * Return a 200 OK application/binary response
     * @param is The stream to copy
     */
    protected static void renderBinary(InputStream is) {
        throw new RenderBinary(is, null);
    }

    /**
     * Return a 200 OK application/binary response with content-disposition attachment
     * @param is The stream to copy
     * @param name The attachment name
     */
    protected static void renderBinary(InputStream is, String name) {
        throw new RenderBinary(is, name);
    }
    
    /**
     * Return a 200 OK application/binary response
     * @param file The file to copy
     */
    protected static void renderBinary(File file) {
        throw new RenderBinary(file, null);
    }
    
    /**
     * Return a 200 OK application/binary response with content-disposition attachment
     * @param file The file to copy
     * @param name The attachment name
     */
    protected static void renderBinary(File file, String name) {
        throw new RenderBinary(file, name);
    }

    /**
     * Render a 200 OK application/json response
     * @param jsonString The JSON string
     */
    protected static void renderJSON(String jsonString) {
        throw new RenderJson(jsonString);
    }
    
    /**
     * Render a 200 OK application/json response
     * @param o The Java object to serialize
     * @param includes Object properties to include
     */
    protected static void renderJSON(Object o, String... includes) {
        throw new RenderJson(o, includes);
    }

    /**
     * Send a 302 Redirect response
     * @param url The Location to redirect
     */
    protected static void redirect(String url) {
        throw new Redirect(url);
    }

    /**
     * Send a 401 Unauthorized response
     * @param realm The realm name
     */
    protected static void unauthorized(String realm) {
        throw new Unauthorized(realm);
    }

    /**
     * Send a 404 Not Found reponse
     * @param what The Not Found resource name
     */
    protected static void notFound(String what) {
        throw new NotFound(what);
    }

    /**
     * Send a 404 Not Found reponse if object is null
     * @param o The object to check
     */
    protected static void notFoundIfNull(Object o) {
        if (o == null) {
            notFound();
        }
    }

    /**
     * Send a 404 Not Found reponse
     */
    protected static void notFound() {
        throw new NotFound("");
    }

    /**
     * Send a 403 Forbidden response
     * @param reason The reason
     */
    protected static void forbidden(String reason) {
        throw new Forbidden(reason);
    }

    /**
     * Send a 403 Forbidden response
     */
    protected static void forbidden() {
        throw new Forbidden("Access denied");
    }

    /**
     * Send a 5xx Error response
     * @param status The exact status code
     * @param reason The reason
     */
    protected static void error(int status, String reason) {
        throw new Error(status, reason);
    }

    /**
     * Send a 500 Error response
     * @param reason The reason
     */
    protected static void error(String reason) {
        throw new Error(reason);
    }

    /**
     * Send a 500 Error response
     */
    protected static void error() {
        throw new Error("Internal Error");
    }

    /**
     * Add a value to the flash scope
     * @param key The key
     * @param value The value
     */
    protected static void flash(String key, Object value) {
        Scope.Flash.current().put(key, value);
    }

    /**
     * Redirect to another action
     * @param action The fully qualified action name (ex: Application.index)
     * @param args Method arguments
     */
    protected static void redirect(String action, Object... args) {
        try {
            Map<String, Object> r = new HashMap<String, Object>();
            Method actionMethod = (Method)ActionInvoker.getActionMethod(action)[1];
            String[] names = (String[]) actionMethod.getDeclaringClass().getDeclaredField("$" + actionMethod.getName() + LocalVariablesNamesTracer.computeMethodHash(actionMethod.getParameterTypes())).get(null);
            assert names.length == args.length : "Problem is action redirection";
            for (int i = 0; i < names.length; i++) {
                Unbinder.unBind(r, args[i], names[i]);
            }
            try {
                throw new Redirect(Router.reverse(action, r).toString());
            } catch (NoRouteFoundException e) {
                StackTraceElement element = PlayException.getInterestingStrackTraceElement(e);
                if (element != null) {
                    throw new NoRouteFoundException(action, r, Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber());
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            if (e instanceof Redirect) {
                throw (Redirect) e;
            }
            if (e instanceof PlayException) {
                throw (PlayException) e;
            }
            throw new UnexpectedException(e);
        }
    }

    /**
     * Render a specific template
     * @param templateName The template name
     * @param args The template data
     */
    protected static void renderTemplate(String templateName, Object... args) {
        // Template datas
        Scope.RenderArgs templateBinding = Scope.RenderArgs.current();
        for (Object o : args) {
            List<String> names = LocalVariablesNamesTracer.getAllLocalVariableNames(o);
            for (String name : names) {
                templateBinding.put(name, o);
            }
        }
        templateBinding.put("session", Scope.Session.current());
        templateBinding.put("request", Http.Request.current());
        templateBinding.put("flash", Scope.Flash.current());
        templateBinding.put("params", Scope.Params.current());
        templateBinding.put("play", new Play());
        try {
            Template template = TemplateLoader.load(templateName);
            throw new RenderTemplate(template, templateBinding.data);
        } catch (TemplateNotFoundException ex) {
            StackTraceElement element = PlayException.getInterestingStrackTraceElement(ex);
            if (element != null) {
                throw new TemplateNotFoundException(templateName, Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber());
            } else {
                throw ex;
            }
        }
    }

    /**
     * Render the corresponding template
     * @param templateName The template name
     * @param args The template data
     */
    protected static void render(Object... args) {
        String templateName = null;
        if (args.length > 0 && args[0] instanceof String && LocalVariablesNamesTracer.getAllLocalVariableNames(args[0]).isEmpty()) {
            templateName = args[0].toString();
        } else {
            templateName = Http.Request.current().action.replace(".", "/") + "." + Http.Request.current().format;
        }
        renderTemplate(templateName, args);
    }

    /**
     * Retrieve annotation for the action method
     * @param clazz The annotation class
     * @return Annotation object or null if not found
     */
    public static <T extends Annotation> T getActionAnnotation(Class<T> clazz) {
        Method m = (Method)ActionInvoker.getActionMethod(Http.Request.current().action)[1];
        if (m.isAnnotationPresent(clazz)) {
            return m.getAnnotation(clazz);
        }
        return null;
    }
    
    /**
     * Retrieve annotation for the action method
     * @param clazz The annotation class
     * @return Annotation object or null if not found
     */
    public static Class getControllerClass() {
        return Play.classloader.getClassIgnoreCase("controllers." + Http.Request.current().controller);
    }

}
