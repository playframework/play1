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
 * 500 Error
 */
public class Error extends Result {

    private int status;

    public Error(String reason) {
        super(reason);
        this.status = 500;
    }

    public Error(int status, String reason) {
        super(reason);
        this.status = status;
    }

    public void apply(Request request, Response response) {
        response.status = status;
        response.contentType = "text/html";
        Map<String, Object> binding = Scope.RenderArgs.current().data;
        binding.put("exception", this);
        binding.put("result", this);
        binding.put("session", Scope.Session.current());
        binding.put("request", Http.Request.current());
        binding.put("flash", Scope.Flash.current());
        binding.put("params", Scope.Params.current());
        binding.put("play", new Play());
        String errorHtml = TemplateLoader.load("errors/" + this.status + ".html").render(binding);
        try {
            response.out.write(errorHtml.getBytes("utf-8"));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
}
