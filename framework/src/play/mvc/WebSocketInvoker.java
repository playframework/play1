package play.mvc;

import play.Play;
import play.data.validation.Validation;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;

public class WebSocketInvoker {

    public static void resolve(Http.Request request) {
        ActionInvoker.resolve(request);
    }

    public static void invoke(Http.Request request, Http.Inbound inbound, Http.Outbound outbound) {

        try {

            // 1. Easy debugging ...
            if (Play.mode == Play.Mode.DEV) {
                WebSocketController.class.getDeclaredField("inbound").set(null, Http.Inbound.current());
                WebSocketController.class.getDeclaredField("outbound").set(null, Http.Outbound.current());
                WebSocketController.class.getDeclaredField("params").set(null, Scope.Params.current());
                WebSocketController.class.getDeclaredField("request").set(null, Http.Request.current());
                WebSocketController.class.getDeclaredField("session").set(null, Scope.Session.current());
                WebSocketController.class.getDeclaredField("validation").set(null, Validation.current());
            }

            ActionInvoker.invoke(request, null);

        }catch (PlayException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }

    }
}
