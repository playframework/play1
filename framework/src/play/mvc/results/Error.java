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
 * 500 Error
 */
public class Error extends Result {

    private final int status;

    public Error(String reason) {
        super(reason);
        this.status = Http.StatusCode.INTERNAL_ERROR;
    }

    public Error(int status, String reason) {
        super(reason);
        this.status = status;
    }

    @Override
    public void apply(Request request, Response response) {
        response.status = status;
        String format = request.format;
        if (request.isAjax() && "html".equals(format)) {
            format = "txt";
        }
        response.contentType = MimeTypes.getContentType("xx." + format);
        Map<String, Object> binding = Scope.RenderArgs.current().data;
        binding.put("exception", this);
        binding.put("result", this);
        binding.put("session", Scope.Session.current());
        binding.put("request", Http.Request.current());
        binding.put("flash", Scope.Flash.current());
        binding.put("params", Scope.Params.current());
        binding.put("play", new Play());
        String errorHtml = getMessage();
        try {
            errorHtml = TemplateLoader.load("errors/" + this.status + "." + (format == null ? "html" : format)).render(binding);
        } catch (Exception e) {
            // no template in desired format, just display the default response
        }
        try {
            response.out.write(errorHtml.getBytes(getEncoding()));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public int getStatus() {
        return status;
    }
}
