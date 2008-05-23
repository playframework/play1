package play.mvc;

public class ActionInvoker {
    
    public static void invoke(Http.Request request, Http.Response response) {
        
        Router.route(request);
        
    }

}
