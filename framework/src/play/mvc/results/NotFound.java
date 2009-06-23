package play.mvc.results;

import java.util.Map;

import play.Logger;
import play.Play;
import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope;
import play.templates.TemplateLoader;

/**
 * 404 not found
 */
public class NotFound extends Result {

    private String why;
    
    /**
     * @param why a description of the problem
     */
    public NotFound(String why) {
        super(why);
        this.why = why;
    }

    /**
     * @param method routed method
     * @param path  routed path 
     */
    public NotFound(String method, String path) {
        super(method + " " + path);
    }

    public void apply(Request request, Response response) {
        response.status = 404;
        response.contentType = "text/html";
        Map<String, Object> binding = Scope.RenderArgs.current().data;
        binding.put("result", this);
        binding.put("session", Scope.Session.current());
        binding.put("request", Http.Request.current());
        binding.put("flash", Scope.Flash.current());
        binding.put("params", Scope.Params.current());
        binding.put("play", new Play());
        String errorHtml = TemplateLoader.load("errors/404.html").render(binding);
        try {
            response.out.write(errorHtml.getBytes("utf-8"));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
}
