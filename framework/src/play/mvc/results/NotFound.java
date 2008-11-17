package play.mvc.results;

import java.util.HashMap;
import java.util.Map;

import play.Logger;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.templates.TemplateLoader;

public class NotFound extends Result {
    
    /**
     * 
     * @param why a description of the problem
     */
    public NotFound(String why) {
        super(why);
    }
   
    /**
     * 
     * @param method routed method
     * @param path  routed path 
     */
    public NotFound(String method, String path) {
        super(method + " " + path);
    }

    public void apply(Request request, Response response) {
        response.status = 404;
        Logger.warn("404 -> %s %s", request.method, request.url);
        response.contentType="text/html";
        Map<String, Object> binding = new HashMap<String, Object>();
        binding.put("result",this);
        String errorHtml = TemplateLoader.load("errors/404.html").render(binding);
        try {
			response.out.write(errorHtml.getBytes("utf-8"));
		}catch (Exception e) {
			throw new UnexpectedException(e);
		}
    }
}
