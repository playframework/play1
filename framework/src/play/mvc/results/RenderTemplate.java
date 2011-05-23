package play.mvc.results;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import play.Play;
import play.exceptions.UnexpectedException;
import play.libs.MimeTypes;
import play.libs.optimization.Compression;
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

            setContentTypeIfNotSet(response, contentType);

            if (contentType.startsWith("text/html")
                && Boolean.parseBoolean(Play.configuration.getProperty("optimization.compressHTML"))) {
                content = Compression.compressHTML(content);
            }
            else if (contentType.startsWith("text/css")
                && Boolean.parseBoolean(Play.configuration.getProperty("optimization.compressCSS"))) {
                content = Compression.compressCSS(content);
            }
            else if (contentType.startsWith("text/xml")
                && Boolean.parseBoolean(Play.configuration.getProperty("optimization.compressXML"))) {
                content = Compression.compressXML(content);
            }

            if (gzipIsSupported(request)) {
                final ByteArrayOutputStream gzip = Compression.gzip(content);
                response.setHeader("Content-Encoding", "gzip");
                response.setHeader("Content-Length", gzip.size() + "");
                response.out = gzip;
            } else {
                response.out.write(content.getBytes(getEncoding()));
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public String getContent() {
        return content;
    }

}
