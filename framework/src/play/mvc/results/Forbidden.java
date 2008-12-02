package play.mvc.results;

import java.util.HashMap;
import java.util.Map;

import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
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
        Map<String, Object> binding = new HashMap<String, Object>();
        binding.put("result", this);
        String errorHtml = TemplateLoader.load("errors/403.html").render(binding);
        try {
            response.out.write(errorHtml.getBytes("utf-8"));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
    
}
