package play.mvc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import play.Play;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.SignaturesNamesRepository;
import play.exceptions.JavaExecutionException;
import play.exceptions.NoRouteFoundException;
import play.exceptions.PlayException;
import play.exceptions.TemplateNotFoundException;
import play.mvc.Http.Response;
import play.mvc.results.NotFound;
import play.mvc.results.Redirect;
import play.mvc.results.RenderBinary;
import play.mvc.results.RenderTemplate;
import play.mvc.results.RenderText;
import play.mvc.results.RenderJson;
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

    protected static void renderText(CharSequence text) {
        throw new RenderText(text);
    }

    protected static void renderText(CharSequence pattern, Object... args) {
        throw new RenderText(String.format(pattern.toString(), args));
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

    protected static void redirect(String action, Object... args) {
        Map<String, String> r = new HashMap<String, String>();
        String[] names = SignaturesNamesRepository.get(ActionInvoker.getActionMethod(action));
        assert names.length == args.length : "Problem is action redirection";
        for (int i = 0; i < names.length; i++) {
            r.put(names[i], args[i] == null ? null : args[i].toString());
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
    }

    protected static void render(Object... args) {
        // Template datas
        Scope.RenderArgs templateBinding = Scope.RenderArgs.current();
        for (Object o : args) {
            String name = LocalVariablesNamesTracer.getLocalVariableName(o);
            if (name != null) {
                templateBinding.put(name, o);
            }
        }
        templateBinding.put("session", Scope.Session.current());
        templateBinding.put("request", Http.Request.current());
        templateBinding.put("flash", Scope.Flash.current());
        templateBinding.put("params", Scope.Params.current());
        templateBinding.put("play", new Play());
        // Template name
        String templateName = null;
        if (args.length > 0 && args[0] instanceof String && LocalVariablesNamesTracer.getLocalVariableName(args[0]) == null) {
            templateName = args[0].toString();
        } else {
            templateName = Http.Request.current().action.replace(".", "/") + "." + Http.Request.current().format;
        }
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
}
