package play.mvc.results;

import java.io.ByteArrayOutputStream;
import play.exceptions.UnexpectedException;
import play.libs.optimization.Compression;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

/**
 * 200 OK with a text/plain
 */
public class RenderHtml extends Result {
    
    String text;
    
    public RenderHtml(CharSequence text) {
        this.text = text.toString();
    }

    public void apply(Request request, Response response) {
        try {
            setContentTypeIfNotSet(response, "text/html");

            if (Boolean.parseBoolean(play.Play.configuration.getProperty("optimization.compressHTML"))) {
                text = Compression.compressHTML(text);
            }

            if (gzipIsSupported(request)) {
                final ByteArrayOutputStream gzip = Compression.gzip(text);
                response.setHeader("Content-Encoding", "gzip");
                response.setHeader("Content-Length", gzip.size() + "");
                response.out = gzip;
            } else {
                response.out.write(text.getBytes(getEncoding()));
            }
        } catch(Exception e) {
            throw new UnexpectedException(e);
        }
    }

}
