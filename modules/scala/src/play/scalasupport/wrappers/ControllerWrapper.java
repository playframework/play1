package play.scalasupport.wrappers;

import play.mvc.Controller;

public class ControllerWrapper extends Controller {
    
    public static void render(Object... args) {
        Controller.render(args);
    }
    
    public static void renderText(Object text) {
        Controller.renderText(text);
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

}
