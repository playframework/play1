package play.mvc;

import play.mvc.Http.Response;
import play.mvc.results.RenderText;

public abstract class Controller {
    
    public static final Http.Request request = null;
    public static final Response response = null;
    public static final Scope.Session session = null;
    public static final Scope.Flash flash = null;
    public static final Scope.Params params = null;
    public static final Scope.RenderArgs renderArgs = null;
    
    public static void renderText(CharSequence text) {
        throw new RenderText(text);
    }
    
    public static void renderText(CharSequence pattern, Object... args) {
        throw new RenderText(String.format(pattern.toString(), args));
    }

}
