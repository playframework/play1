package play.mvc;

import java.io.File;
import java.io.InputStream;
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

public abstract class Controller {

    public static Http.Request request = null;
    public static Response response = null;
    public static Scope.Session session = null;
    public static Scope.Flash flash = null;
    public static Scope.Params params = null;
    public static Scope.RenderArgs renderArgs = null;

    protected static void renderText(Object text) {
        throw new RenderText(text == null ? "" : text.toString());
    }

    protected static void renderText(CharSequence pattern, Object... args) {
        throw new RenderText(String.format(pattern.toString(), args));
    }

    protected static void renderXml(String xml) {
        throw new RenderXml(xml);
    }

    protected static void renderXml(Document xml) {
        throw new RenderXml(xml);
    }

    protected static void renderBinary(InputStream is) {
        throw new RenderBinary(is, null);
    }

    protected static void renderBinary(InputStream is, String name) {
        throw new RenderBinary(is, name);
    }

    protected static void renderBinary(File file) {
        throw new RenderBinary(file, null);
    }

    protected static void renderBinary(File file, String name) {
        throw new RenderBinary(file, name);
    }

    protected static void renderJSON(String jsonString) {
        throw new RenderJson(jsonString);
    }

    protected static void renderJSON(Object o, String... includes) {
        throw new RenderJson(o, includes);
    }

    protected static void redirect(String url) {
        throw new Redirect(url);
    }

    protected static void unauthorized(String realm) {
        throw new Unauthorized(realm);
    }

    protected static void notFound(String what) {
        throw new NotFound(what);
    }

    protected static void notFoundIfNull(Object o) {
        if (o == null) {
            notFound();
        }
    }

    protected static void notFound() {
        throw new NotFound("");
    }

    protected static void forbidden(String reason) {
        throw new Forbidden(reason);
    }

    protected static void forbidden() {
        throw new Forbidden("Access denied");
    }

    protected static void error(Throwable throwable) {
        throw new Error(throwable);
    }

    protected static void error(int status, String reason) {
        throw new Error(status, reason);
    }

    protected static void error(String reason) {
        throw new Error(reason);
    }

    protected static void error() {
        throw new Error("Internal Error");
    }

    protected static void flash(String key, String value) {
        Scope.Flash.current().put(key, value);
    }

    protected static void redirect(String action, Object... args) {
        try {
            Map<String, Object> r = new HashMap<String, Object>();
            String[] names = (String[]) ActionInvoker.getActionMethod(action).getDeclaringClass().getDeclaredField("$" + ActionInvoker.getActionMethod(action).getName() + LocalVariablesNamesTracer.computeMethodHash(ActionInvoker.getActionMethod(action).getParameterTypes())).get(null);
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

    protected static void render(Object... args) {
        String templateName = null;
        if (args.length > 0 && args[0] instanceof String && LocalVariablesNamesTracer.getAllLocalVariableNames(args[0]).isEmpty()) {
            templateName = args[0].toString();
        } else {
            templateName = Http.Request.current().action.replace(".", "/") + "." + Http.Request.current().format;
        }
        renderTemplate(templateName, args);
    }
}
