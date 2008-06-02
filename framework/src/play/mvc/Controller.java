
package play.mvc;

import java.util.HashMap;
import java.util.Map;
import play.Play;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.SignaturesNamesRepository;
import play.exceptions.NoRouteFoundException;
import play.exceptions.PlayException;
import play.exceptions.TemplateNotFoundException;
import play.mvc.Http.Response;
import play.mvc.results.Redirect;
import play.mvc.results.RenderTemplate;
import play.mvc.results.RenderText;
import play.templates.Template;
import play.templates.TemplateLoader;

public abstract class Controller {
    
    public static final Http.Request request = null;
    public static final Response response = null;
    public static final Scope.Session session = null;
    public static final Scope.Flash flash = null;
    public static final Scope.Params params = null;
    public static final Scope.RenderArgs renderArgs = null;
    
    protected static void renderText(CharSequence text) {
        throw new RenderText(text);
    }
    
    protected static void renderText(CharSequence pattern, Object... args) {
        throw new RenderText(String.format(pattern.toString(), args));
    }
    
    protected static void redirect(String url) {
        throw new Redirect(url);
    }
    
    protected static void redirect(String action, Object... args) {
        Map<String, String> r = new HashMap<String, String>();
        String[] names = SignaturesNamesRepository.get(ActionInvoker.getActionMethod(action));
        assert names.length == args.length : "Problem is action redirection";
        for(int i=0; i<names.length; i++) {
            r.put(names[i], args[i] == null ? null : args[i].toString());
        }
        try {
            throw new Redirect(Router.reverse(action, r).toString());
        } catch(NoRouteFoundException e) {
            StackTraceElement element = PlayException.getInterestingStrackTraceElement(e);
            if(element != null) {
                throw new NoRouteFoundException(action, r, Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber());
            } else {
                throw e;
            }
        }
    }
    
    protected static void render(Object... args) {
        // Template datas
        Scope.RenderArgs templateBinding = Scope.RenderArgs.current();
        for(Object o:args) {
            String name = LocalVariablesNamesTracer.getLocalVariableName(o);
            if(name != null) {
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
        if(args.length>0 && args[0] instanceof String && LocalVariablesNamesTracer.getLocalVariableName(args[0]) == null) {
            templateName = args[0].toString();
        } else {
            templateName = Http.Request.current().action.replace(".", "/")+"."+Http.Request.current().format;
        }
        try {
            Template template = TemplateLoader.load(templateName);
            throw new RenderTemplate(template, templateBinding.data);
        } catch(TemplateNotFoundException ex) {
            StackTraceElement element = PlayException.getInterestingStrackTraceElement(ex);
            if(element != null) {
                throw new TemplateNotFoundException(templateName, Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber());
            } else {
                throw ex;
            }
        }      
    }

}
