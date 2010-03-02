package play.mvc;

import com.google.gson.JsonSerializer;
import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import org.w3c.dom.Document;
import play.Invoker.Suspend;
import play.Logger;
import play.Play;
import play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation;
import play.classloading.enhancers.ControllersEnhancer.ControllerSupport;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesSupport;
import play.data.binding.Binder;
import play.data.validation.Validation;
import play.exceptions.NoRouteFoundException;
import play.exceptions.PlayException;
import play.exceptions.TemplateNotFoundException;
import play.exceptions.UnexpectedException;
import play.utils.Java;
import play.libs.Time;
import play.mvc.Http.Request;
import play.mvc.Router.ActionDefinition;
import play.mvc.results.Error;
import play.mvc.results.Forbidden;
import play.mvc.results.NotFound;
import play.mvc.results.NotModified;
import play.mvc.results.Ok;
import play.mvc.results.Redirect;
import play.mvc.results.RedirectToStatic;
import play.mvc.results.RenderBinary;
import play.mvc.results.RenderTemplate;
import play.mvc.results.RenderText;
import play.mvc.results.RenderJson;
import play.mvc.results.RenderXml;
import play.mvc.results.Result;
import play.mvc.results.Unauthorized;
import play.templates.Template;
import play.templates.TemplateLoader;
import play.utils.Default;
import play.vfs.VirtualFile;

/**
 * Application controller support
 */
public class Controller implements ControllerSupport, LocalVariablesSupport {

    /**
     * The current HTTP request
     */
    protected static Http.Request request = null;
    /**
     * The current HTTP response
     */
    protected static Http.Response response = null;
    /**
     * The current HTTP session
     */
    protected static Scope.Session session = null;
    /**
     * The current flash scope
     */
    protected static Scope.Flash flash = null;
    /**
     * The current HTTP params
     */
    protected static Scope.Params params = null;
    /**
     * The current renderArgs (used in templates)
     */
    protected static Scope.RenderArgs renderArgs = null;
    /**
     * The current Validation
     */
    protected static Validation validation = null;

    /**
     * Return a 200 OK text/plain response
     * @param text The response content
     */
    protected static void renderText(Object text) {
        throw new RenderText(text == null ? "" : text.toString());
    }

    /**
     * Return a 200 OK text/plain response
     * @param pattern The response content to be formatted (with String.format)
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
        throw new RenderBinary(is, null, true);
    }
    
    /**
     * Return a 200 OK application/binary response. Content is streamed
     * @param is The stream to copy
     */
    protected static void renderBinary(InputStream is, long length) {
        throw new RenderBinary(is, null, length, true);
    }

    /**
     * Return a 200 OK application/binary response with content-disposition attachment
     * @param is The stream to copy
     * @param name The attachment name
     */
    protected static void renderBinary(InputStream is, String name) {
        throw new RenderBinary(is, name, false);
    }
    
    /**
     * Return a 200 OK application/binary response with content-disposition attachment
     * @param is The stream to copy. COntent is streamed
     * @param name The attachment name
     */
    protected static void renderBinary(InputStream is, String name, long length) {
        throw new RenderBinary(is, name, length, false);
    }

    /**
     * Return a 200 OK application/binary response with content-disposition attachment
     * @param is The stream to copy
     * @param name The attachment name
     * @param inline true to set the response Content-Disposition to inline
     */
    protected static void renderBinary(InputStream is, String name, boolean inline) {
        throw new RenderBinary(is, name, inline);
    }
    
    /**
     * Return a 200 OK application/binary response with content-disposition attachment
     * @param is The stream to copy
     * @param name The attachment name
     * @param inline true to set the response Content-Disposition to inline
     */
    protected static void renderBinary(InputStream is, String name, long length, boolean inline) {
        throw new RenderBinary(is, name, length, inline);
    }

    /**
     * Return a 200 OK application/binary response with content-disposition attachment
     * @param is The stream to copy
     * @param name The attachment name
     * @param contentType The content type of the attachment
     * @param inline true to set the response Content-Disposition to inline
     */
    protected static void renderBinary(InputStream is, String name, String contentType, boolean inline) {
        throw new RenderBinary(is, name, contentType, inline);
    }
    
    /**
     * Return a 200 OK application/binary response with content-disposition attachment
     * @param is The stream to copy
     * @param name The attachment name
     * @param contentType The content type of the attachment
     * @param inline true to set the response Content-Disposition to inline
     */
    protected static void renderBinary(InputStream is, String name, long length, String contentType, boolean inline) {
        throw new RenderBinary(is, name, length, contentType, inline);
    }

    /**
     * Return a 200 OK application/binary response
     * @param file The file to copy
     */
    protected static void renderBinary(File file) {
        throw new RenderBinary(file);
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
     */
    protected static void renderJSON(Object o) {
        throw new RenderJson(o);
    }
    
        /**
     * Render a 200 OK application/json response
     * @param o The Java object to serialize
     * @param adapters A set of GSON serializers/deserializers/instance creator to use
     */
    protected static void renderJSON(Object o, JsonSerializer... adapters) {
        throw new RenderJson(o, adapters);
    }

    /**
     * Send a 304 Not Modified response
     */
    protected static void notModified() {
        throw new NotModified();
    }

    /**
     * Send a 401 Unauthorized response
     * @param realm The realm name
     */
    protected static void unauthorized(String realm) {
        throw new Unauthorized(realm);
    }

    /**
     * Send a 404 Not Found response
     * @param what The Not Found resource name
     */
    protected static void notFound(String what) {
        throw new NotFound(what);
    }

    /**
     * Send a 200 OK response
     */
    protected static void ok() {
        throw new Ok();
    }

    /**
     * Send a TODO response
     */
    protected static void todo() {
        notFound("This action has not been implemented Yet (" + request.action + ")");
    }

    /**
     * Send a 404 Not Found response if object is null
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
     * @param reason The reason
     */
    protected static void error(Exception reason) {
        Logger.error(reason, "error()");
        throw new Error(reason.toString());
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
     * Send a 302 redirect response.
     * @param url The Location to redirect
     */
    protected static void redirect(String url) {
        redirect(url, false);
    }

    /**
     * Send a 302 redirect response.
     * @param file The Location to redirect
     */
    protected static void redirectToStatic(String file) {
        try {
            VirtualFile vf = Play.getVirtualFile(file);
            if (vf == null || !vf.exists()) {
                throw new NoRouteFoundException(file);
            }
            throw new RedirectToStatic(Router.reverse(Play.getVirtualFile(file)));
        } catch (NoRouteFoundException e) {
            StackTraceElement element = PlayException.getInterestingStrackTraceElement(e);
            if (element != null) {
                throw new NoRouteFoundException(file, Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber());
            } else {
                throw e;
            }
        }
    }

    /**
     * Send a Redirect response.
     * @param url The Location to redirect
     * @param permanent true -> 301, false -> 302
     */
    protected static void redirect(String url, boolean permanent) {
        if (url.matches("^([^./]+[.]?)+$")) { // fix Java !
            redirect(url, permanent, new Object[0]);
        }
        throw new Redirect(url, permanent);
    }

    /**
     * 302 Redirect to another action
     * @param action The fully qualified action name (ex: Application.index)
     * @param args Method arguments
     */
    public static void redirect(String action, Object... args) {
        redirect(action, false, args);
    }

    /**
     * Redirect to another action
     * @param action The fully qualified action name (ex: Application.index)
     * @param permanent true -> 301, false -> 302
     * @param args Method arguments
     */
    public static void redirect(String action, boolean permanent, Object... args) {
        try {
            Map<String, Object> r = new HashMap<String, Object>();
            Method actionMethod = (Method) ActionInvoker.getActionMethod(action)[1];
            String[] names = (String[]) actionMethod.getDeclaringClass().getDeclaredField("$" + actionMethod.getName() + LocalVariablesNamesTracer.computeMethodHash(actionMethod.getParameterTypes())).get(null);
            for (int i = 0; i < names.length && i< args.length; i++) {
                boolean isDefault = false;
                try {
                    Method defaultMethod = actionMethod.getDeclaringClass().getDeclaredMethod(actionMethod.getName()+"$default$"+(i+1));
                    // Patch for scala defaults
                    if(!Modifier.isStatic(actionMethod.getModifiers()) && actionMethod.getDeclaringClass().getSimpleName().endsWith("$")) {
                        Object instance = actionMethod.getDeclaringClass().getDeclaredField("MODULE$").get(null);
                        if(defaultMethod.invoke(instance).equals(args[i])) {
                            isDefault = true;
                        }
                    }                    
                } catch(NoSuchMethodException e) {
                    //
                }
                if(isDefault) {
                    r.put(names[i], new Default(args[i]));
                } else {
                    r.put(names[i], args[i]);
                }
            }
            try {

                ActionDefinition actionDefinition = Router.reverse(action, r);
                if(_currentReverse.get() != null) {
                    ActionDefinition currentActionDefinition =  _currentReverse.get();
                    currentActionDefinition.action = actionDefinition.action;
                    currentActionDefinition.url = actionDefinition.url;
                    currentActionDefinition.method = actionDefinition.method;
                    currentActionDefinition.star = actionDefinition.star;
                    currentActionDefinition.args = actionDefinition.args;
                    _currentReverse.remove();
                } else {
                    throw new Redirect(actionDefinition.toString(), permanent);
                }
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
        try {
            templateBinding.put("errors", Validation.errors());
        } catch (Exception ex) {
            throw new UnexpectedException(ex);
        }
        try {
            Template template = TemplateLoader.load(templateName);
            throw new RenderTemplate(template, templateBinding.data);
        } catch (TemplateNotFoundException ex) {
            if(ex.isSourceAvailable()) {
                throw ex;
            }
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
     * @param args The template data
     */
    protected static void render(Object... args) {
        String templateName = null;
        final Request request = Request.current();
        final String format = request.format;

        if (args.length > 0 && args[0] instanceof String && LocalVariablesNamesTracer.getAllLocalVariableNames(args[0]).isEmpty()) {
            templateName = args[0].toString();
        } else {
            templateName = request.action.replace(".", "/") + "." + (format == null ? "html" : format);
        }
        if(templateName.startsWith("@")) {
            templateName = templateName.substring(1);
            if(!templateName.contains(".")) {
                templateName = request.controller + "." + templateName;
            }
            templateName = templateName.replace(".", "/") + "." + (format == null ? "html" : format);
        }
        renderTemplate(templateName, args);
    }

    /**
     * Retrieve annotation for the action method
     * @param clazz The annotation class
     * @return Annotation object or null if not found
     */
    protected static <T extends Annotation> T getActionAnnotation(Class<T> clazz) {
        Method m = (Method) ActionInvoker.getActionMethod(Http.Request.current().action)[1];
        if (m.isAnnotationPresent(clazz)) {
            return m.getAnnotation(clazz);
        }
        return null;
    }

    /**
     * Retrieve annotation for the controller class
     * @param clazz The annotation class
     * @return Annotation object or null if not found
     */
    protected static <T extends Annotation> T getControllerAnnotation(Class<T> clazz) {
        if (getControllerClass().isAnnotationPresent(clazz)) {
            return (T)getControllerClass().getAnnotation(clazz);
        }
        return null;
    }
    
    /**
     * Retrieve annotation for the controller class
     * @param clazz The annotation class
     * @return Annotation object or null if not found
     */
    protected static <T extends Annotation> T getControllerInheritedAnnotation(Class<T> clazz) {
        Class c = getControllerClass();
        while(!c.equals(Object.class)) {
            if (c.isAnnotationPresent(clazz)) {
                return (T)c.getAnnotation(clazz);
            }
            c = c.getSuperclass();
        }
        return null;
    }

    /**
     * Retrieve annotation for the action method
     * @return Annotation object or null if not found
     */
    protected static Class getControllerClass() {
        return Http.Request.current().controllerClass;
    }

    /**
     * Call the parent action adding this objects to the params scope
     */
    protected static void parent(Object... args) {
        Map<String, Object> map = new HashMap();
        for (Object o : args) {
            List<String> names = LocalVariablesNamesTracer.getAllLocalVariableNames(o);
            for (String name : names) {
                map.put(name, o);
            }
        }
        parent(map);
    }

    /**
     * Call the parent method
     */
    protected static void parent() {
        parent(new HashMap());
    }

    /**
     * Call the parent action adding this objects to the params scope
     */
    protected static void parent(Map<String, Object> map) {
        try {
            Method method = Http.Request.current().invokedMethod;
            String name = method.getName();
            Class clazz = method.getDeclaringClass().getSuperclass();
            Method superMethod = null;
            while (!clazz.getName().equals("play.mvc.Controller") && !clazz.getName().equals("java.lang.Object")) {
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().equalsIgnoreCase(name) && Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers())) {
                        superMethod = m;
                        break;
                    }
                }
                if (superMethod != null) {
                    break;
                }
                clazz = clazz.getSuperclass();
            }
            if (superMethod == null) {
                throw new RuntimeException("PAF");
            }
            Map<String, String> mapss = new HashMap();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
            	Object value = entry.getValue();
                mapss.put(entry.getKey(),value == null ? null : value.toString());
            }
            Scope.Params.current().__mergeWith(mapss);
            ControllerInstrumentation.initActionCall();
            Java.invokeStatic(superMethod, ActionInvoker.getActionMethodArgs(superMethod, null));
        } catch (InvocationTargetException ex) {
            // It's a Result ? (expected)
            if (ex.getTargetException() instanceof Result) {
                throw (Result) ex.getTargetException();
            } else {
                throw new RuntimeException(ex.getTargetException());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Suspend the current request for a specified amount of time
     */
    protected static void suspend(String timeout) {
    	suspend(1000 * Time.parseDuration(timeout));
    }

    /**
     * Suspend the current request for a specified amount of time (in milliseconds)
     */
    protected static void suspend(int millis) {
        Request.current().isNew = false;
        throw new Suspend(millis);
    }

    /**
     * Suspend this request and wait for the task completion
     */
    protected static void waitFor(Future task) {
        Request.current().isNew = false;
        throw new Suspend(task);
    }

    /**
     * Don't use this directly if you don't know why
     */
    public static ThreadLocal<ActionDefinition> _currentReverse = new ThreadLocal<ActionDefinition>();

    protected static ActionDefinition reverse() {
        ActionDefinition actionDefinition = new ActionDefinition();
        _currentReverse.set(actionDefinition);
        return actionDefinition;
    }
    
}
