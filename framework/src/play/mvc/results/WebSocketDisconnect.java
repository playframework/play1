package play.mvc.results;

import play.mvc.Http.Inbound;
import play.mvc.Http.Outbound;
import play.mvc.Http.Request;

public class WebSocketDisconnect extends WebSocketResult {

    @Override
    public void apply(Request request, Inbound inbound, Outbound outbound) {
    }

}
