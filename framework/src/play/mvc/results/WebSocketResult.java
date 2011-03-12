package play.mvc.results;

import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

/**
 * WebSocket Result support
 */
public abstract class WebSocketResult extends Result {

    public WebSocketResult() {
        super();
    }

    public abstract void apply(Http.Request request, Http.Inbound inbound, Http.Outbound outbound);

    @Override
    public void apply(Request request, Response response) {
        // Do something, we just want to reuse the underlying result mechanism
    }

}
