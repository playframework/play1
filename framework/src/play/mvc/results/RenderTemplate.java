package play.mvc.results;

import java.util.Map;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.templates.Template;

public class RenderTemplate extends Result {
    
    private Template template;
    Map<String,Object> args;
    
    public RenderTemplate(Template template, Map<String,Object> args) {
        this.template = template;
        this.args = args;
    }

    public void apply(Request request, Response response) {
        String content = template.render(args);
        try {
            setContentTypeIfNotSet(response, "text/html");
            response.out.write(content.getBytes("utf-8"));
        } catch(Exception e) {
            throw new UnexpectedException(e);
        }
    }

}
