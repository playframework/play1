package play.mvc.results;

import java.util.Map;

import play.Play;
import play.exceptions.UnexpectedException;
import play.libs.MimeTypes;
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
        response.status = Http.StatusCode.FORBIDDEN;
        String format = request.format;
        if(request.isAjax() && "html".equals(format)) {
            format = "txt";
        }
        response.contentType = MimeTypes.getContentType("xx."+format);
        Map<String, Object> binding = Scope.RenderArgs.current().data;
        binding.put("result", this);
        binding.put("session", Scope.Session.current());
        binding.put("request", Http.Request.current());
        binding.put("flash", Scope.Flash.current());
        binding.put("params", Scope.Params.current());
        binding.put("play", new Play());
        String errorHtml = getMessage();
        try {
            errorHtml = TemplateLoader.load("errors/403."+(format == null ? "html" : format)).render(binding);
        } catch(Exception e) {
        }
        try {
            response.out.write(errorHtml.getBytes(getEncoding()));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
    
}
