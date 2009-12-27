package play.modules.grizzly;

import com.sun.grizzly.arp.AsyncExecutor;
import java.util.HashMap;
import java.util.Map;
import play.Invoker.Invocation;
import play.Play;
import play.Play.Mode;
import play.PlayPlugin;
import play.mvc.ActionInvoker;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;

public class GrizzlyInvocation extends Invocation {

    Map<String, RenderStatic> staticPathsCache = new HashMap();
    AsyncExecutor asyncExecutor;

    public GrizzlyInvocation(AsyncExecutor ae) {
        this.asyncExecutor = ae;
    }

    @Override
    public boolean init() {
        try {
            asyncExecutor.preExecute();
            asyncExecutor.execute();
            Request request = Request.current();
            boolean raw = false;
            for (PlayPlugin plugin : Play.plugins) {
                if (plugin.rawInvocation(request, Response.current())) {
                    raw = true;
                    break;
                }
            }
            if (raw) {
                PlayAdapter.copyResponse();
                return false;
            }
            // Patch favicon.ico
            if (!request.path.equals("/favicon.ico")) {
                super.init();
            }
            if (Play.mode == Mode.PROD && staticPathsCache.containsKey(request.path)) {
                PlayAdapter.serveStatic(staticPathsCache.get(request.path));
                return false;
            }
            try {
                Router.routeOnlyStatic(Request.current());
            } catch (NotFound e) {
                PlayAdapter.serve404(e);
                return false;
            } catch (RenderStatic e) {
                if (Play.mode == Mode.PROD) {
                    staticPathsCache.put(request.path, e);
                }
                PlayAdapter.serveStatic(e);
                return false;
            }
            return true;
        } catch (Exception e) {
            PlayAdapter.serve500(e);
        }
        return true;
    }

    @Override
    public void run() {
        try {
            super.run();
        } catch (Exception e) {
            PlayAdapter.serve500(e);
            return;
        }
    }

    @Override
    public void execute() throws Exception {
        ActionInvoker.invoke(Request.current(), Response.current());
        PlayAdapter.copyResponse();
    }

    @Override
    public void _finally() {
        try {
            super._finally();
        } finally {
            try {
                asyncExecutor.postExecute();
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String toString() {
        return "Request " + Request.current();
    }
}
