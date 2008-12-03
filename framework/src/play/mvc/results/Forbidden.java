package play.mvc.results;

import java.util.Map;

import play.Play;
import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope;
import play.templates.TemplateLoader;

/**
 * 403 Forbidden
 */
public class Forbidden extends Result {

    public Forbidden(String reason) {
        super(reason);
    }

    public void apply(Request request, Response response) {
        response.status = 403;
        response.contentType = "text/html";
        Map<String, Object> binding = Scope.RenderArgs.current().data;
        binding.put("result", this);
        binding.put("session", Scope.Session.current());
        binding.put("request", Http.Request.current());
        binding.put("flash", Scope.Flash.current());
        binding.put("params", Scope.Params.current());
        binding.put("play", new Play());
        String errorHtml = TemplateLoader.load("errors/403.html").render(binding);
        try {
            response.out.write(errorHtml.getBytes("utf-8"));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
    
}
