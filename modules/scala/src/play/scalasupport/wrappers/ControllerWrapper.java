package play.scalasupport.wrappers;

import java.io.InputStream;
import play.mvc.Controller;

public class ControllerWrapper extends Controller {
    
    public static void render(Object... args) {
        Controller.render(args);
    }
    
    public static void renderText(Object text) {
        Controller.renderText(text);
    }

    public static void renderXml(String xml) {
        Controller.renderXml(xml);
    }

    public static void renderJSON(String json) {
        Controller.renderJSON(json);
    }
    
    public static void renderText(CharSequence pattern, Object... args) {
        Controller.renderText(pattern, args);
    }
    
    public static void redirect(String url) {
        Controller.redirect(url);
    }

    public static void redirectToStatic(String file) {
        Controller.redirectToStatic(file);
    }

    public static void redirect(String url, boolean permanent) {
        Controller.redirect(url, permanent);
    }

    public static void redirect(String action, Object... args) {
       Controller.redirect(action, args);
    }

    public static void redirect(String action, boolean permanent, Object... args) {
        Controller.redirect(action, permanent, args);
    }

    public static void unauthorized(String realm) {
        Controller.unauthorized(realm);
    }

    public static void notFound(String what) {
        Controller.notFound(what);
    }

    public static void notFoundIfNull(Object o) {
        Controller.notFoundIfNull(o);
    }

    public static void ok() {
        Controller.ok();
    }

    public static void renderBinary(InputStream stream) {
        Controller.renderBinary(stream);
    }

    public static void forbidden() {
        Controller.forbidden();
    }

}
