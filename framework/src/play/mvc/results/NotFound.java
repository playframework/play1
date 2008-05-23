package play.mvc.results;

import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Result;

public class NotFound extends Result {

    public void apply(Request request, Response response) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
