package play.mvc.results;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringBufferInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import play.Logger;
import play.Play;
import play.libs.MimeTypes;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.templates.Template;

/**
 * 200 OK with a template rendering
 */
public class RenderTemplate extends Result {

    public Template template;
    private String content;
    Map<String, Object> args;

    public RenderTemplate(Template template, Map<String, Object> args) {
        this.template = template;
        this.args = args;
        this.content = template.render(args);
    }

    public void apply(Request request, Response response) {
        try {
            final String contentType = MimeTypes.getContentType(template.name, "text/plain");
            response.out.write(content.getBytes("utf-8"));
            setContentTypeIfNotSet(response, contentType);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public String getContent() {
        return content;
    }

}
        
