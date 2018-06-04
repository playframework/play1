package play.mvc;

import java.util.concurrent.Future;
import play.classloading.enhancers.ControllersEnhancer.ControllerSupport;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesSupport;
import play.data.validation.Validation;
import play.libs.F;
import play.mvc.results.WebSocketDisconnect;

public class WebSocketController implements ControllerSupport, LocalVariablesSupport, PlayController {

    protected static Http.Request request = null;
    protected static Http.Inbound inbound = null;
    protected static Http.Outbound outbound = null;
    protected static Scope.Params params = null;
    protected static Validation validation = null;
    protected static Scope.Session session = null;

    protected static void await(String timeout) {
        Controller.await(timeout);
    }

    protected static void await(String timeout, F.Action0 callback) {
        Controller.await(timeout, callback);
    }

    protected static void await(int millis) {
        Controller.await(millis);
    }

    protected static void await(int millis, F.Action0 callback) {
        Controller.await(millis, callback);
    }

    protected static <T> T await(Future<T> future) {
        return Controller.await(future);
    }

    protected static <T> void await(Future<T> future, F.Action<T> callback) {
        Controller.await(future, callback);
    }

    protected static void disconnect() {
        throw new WebSocketDisconnect();
    }

}
