package play.mvc.results;

import java.util.Map;

import play.exceptions.UnexpectedException;
import play.libs.MimeTypes;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.templates.Template;

/**
 * 200 OK with a template rendering
 */
public class RenderTemplate extends Result {

    private String name;
    private String content;

    public RenderTemplate(Template template, Map<String, Object> args) {
        this.name = template.name;
        if (args.containsKey("out")) {
            throw new RuntimeException("Assertion failed! args shouldn't contain out");
        }
        this.content = template.render(args);
    }

    public void apply(Request request, Response response) {
        try {
            final String contentType = MimeTypes.getContentType(name, "text/plain");
            response.out.write(content.getBytes(getEncoding()));
            setContentTypeIfNotSet(response, contentType);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public String getContent() {
        return content;
    }

}
