package play.mvc.results;

import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class NotFound extends Result {
    
    public NotFound(String why) {
        super(why);
    }
    
    public NotFound(String method, String path) {
        super(method + " " + path);
    }

    public void apply(Request request, Response response) {
        response.status = 404;
    }
}
