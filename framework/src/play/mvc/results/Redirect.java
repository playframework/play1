package play.mvc.results;

import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

/**
 * 302 Redirect
 */
public class Redirect extends Result {

    public String url;
    public int code=302;
    
    public Redirect(String url) {
        this.url = url;
    }

    public Redirect(String url,boolean permanent) {
        this.url = url;
        if (permanent)
            this.code=301;
    }
    
    public Redirect(String url,int code) {
        this.url = url;
        this.code=code;
    }
    
    public void apply(Request request, Response response) {
        try {
            if (url.startsWith("http")) {
                //
            } else if (url.startsWith("/")) {
                url = String.format("http%s://%s%s%s", request.secure ? "s" : "", request.domain, (request.port == 80 || request.port == 443) ? "" : ":" + request.port, url);
            } else {
                url = String.format("http%s://%s%s%s%s", request.secure ? "s" : "", request.domain, (request.port == 80 || request.port == 443) ? "" : ":" + request.port, request.path, request.path.endsWith("/") ? url : "/" + url);
            }
            response.status = code;
            response.setHeader("Location", url);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
}
