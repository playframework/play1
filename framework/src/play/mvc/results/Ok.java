package play.mvc.results;


import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

/**
 * 200 OK
 */
public class Ok extends Result {

    public Ok() {
        super("OK");
    }

    @Override
    public void apply(Request request, Response response) {
        response.status = Http.StatusCode.OK;
    }
}
