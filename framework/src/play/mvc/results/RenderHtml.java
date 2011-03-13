package play.mvc.results;

import play.exceptions.UnexpectedException;
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
            response.out.write(text.getBytes(getEncoding()));
        } catch(Exception e) {
            throw new UnexpectedException(e);
        }
    }

}
