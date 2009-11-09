package play.mvc.results;

import java.util.Map;
import play.libs.MimeTypes;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.templates.Template;

/**
 * 200 OK with a template rendering
 */
public class RenderTemplate extends Result {
    
    public Template template;
    private String content;
    Map<String,Object> args;
    
    public RenderTemplate(Template template, Map<String,Object> args) {
        this.template = template;
        this.args = args;
        this.content = template.render(args);
    }

    public void apply(Request request, Response response) {
        try {
            Result.setContentTypeIfNotSet(response, MimeTypes.getContentType(template.name, "text/plain"));
            response.out.write(content.getBytes("utf-8"));
        } catch(Exception e) {
            throw new UnexpectedException(e);
        }
    }

}
