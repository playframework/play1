package play.mvc.results;

import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

/**
 * 302 Redirect
 */
public class RedirectToStatic extends Result {

    String file;
    
    public RedirectToStatic(String file) {
        this.file = file;
    }
    
    public void apply(Request request, Response response) {
        try {
            response.status = Http.StatusCode.FOUND;
            response.setHeader("Location", file);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
}
