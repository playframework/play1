package play.mvc.results;

import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class Status extends Result {

    int code;

    public Status(int code) {
        super(code+"");
        this.code = code;
    }

    public void apply(Request request, Response response) {
        response.status = code;
    }
}