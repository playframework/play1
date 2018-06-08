package play.mvc.results;

import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class Status extends Result {

    private final int code;

    public Status(int code) {
        super(code+"");
        this.code = code;
    }

    @Override
    public void apply(Request request, Response response) {
        response.status = code;
    }

    public int getCode() {
        return code;
    }
}