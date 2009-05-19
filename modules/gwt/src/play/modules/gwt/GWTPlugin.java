package play.modules.gwt;

import java.util.logging.Level;
import java.util.logging.Logger;
import play.Play;
import play.PlayPlugin;
import play.exceptions.JavaExecutionException;
import play.exceptions.PlayException;
import play.mvc.Http.Request;
import play.mvc.Router;
import play.mvc.Router.Route;
import play.mvc.results.RedirectToStatic;
import play.mvc.results.RenderText;
import play.mvc.results.Result;

public class GWTPlugin extends PlayPlugin {

    @Override
    public void onRoutesLoaded() {
        boolean useDefault = true;
        for (Route route : Router.routes) {
            if (route.path.contains("gwt-public")) {
                useDefault = false;
                break;
            }
        }
        if (useDefault) {
            Router.addRoute("GET", "/app", "staticDir:gwt-public");
        }
    }

    @Override
    public void routeRequest(Request request) {
        if (request.path.equals("/@gwt")) {
            throw new RedirectToStatic(Router.reverse(Play.getVirtualFile("/gwt-public/index.html")));
        }
        // Hand made routing;
        if (request.method == "POST") {
            for (Class service : Play.classloader.getAnnotatedClasses(GWTServicePath.class)) {
                String path = ((GWTServicePath) service.getAnnotation(GWTServicePath.class)).value();
                if (request.path.equals(path)) {
                    invokeService(service);
                    break;
                }
            }
        }
    }

    public void invokeService(Class service) {
        String result = "";
        if (GWTService.class.isAssignableFrom(service)) {
            try {
                result = ((GWTService) service.newInstance()).invoke();
            } catch (Exception ex) {
                // Rethrow the enclosed exception
                if (ex instanceof PlayException) {
                    throw (PlayException) ex;
                }
                StackTraceElement element = PlayException.getInterestingStrackTraceElement(ex);
                if (element != null) {
                    throw new JavaExecutionException(Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber(), ex);
                }
                throw new JavaExecutionException(ex);
            }
        }
        throw new RenderText(result);
    }
}
