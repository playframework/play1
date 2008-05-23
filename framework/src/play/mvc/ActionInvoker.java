package play.mvc;

import java.lang.reflect.Method;
import play.Logger;
import play.Play;
import play.libs.Java;

public class ActionInvoker {
    
    public static void invoke(Http.Request request, Http.Response response) {        
        try {
            
            Router.route(request);
            
            try {
                String controller = "controllers."+request.action.substring(0, request.action.lastIndexOf("."));
                String action = request.action.substring(request.action.lastIndexOf(".")+1);
            
                Class controllerClass = Play.classloader.loadClass(controller);
                Method actionMethod = Java.findPublicStaticMethod(action, controllerClass);
                
                if(actionMethod == null) {
                    throw new Exception("Not found");
                }
                
                actionMethod.invoke(null);
                        
            } catch(Exception e) {
                // ActionNotFound
                throw new RuntimeException(e);
            }
            
            
        } catch(Result result) {
            Logger.warn("%s", result);
        }        
    }

}
