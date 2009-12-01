package play.mvc.results;

import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class NoResult extends Result{

    @Override
    public void apply(Request request, Response response) {
    }

}
