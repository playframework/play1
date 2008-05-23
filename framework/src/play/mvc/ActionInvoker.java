package play.mvc;

import play.mvc.results.Result;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import play.Play;
import play.libs.Java;

public class ActionInvoker {

    public static void invoke(Http.Request request, Http.Response response) {
        try {
            
            Http.Request.current.set(request);
            Http.Response.current.set(response);

            // 1. Route the request
            Router.route(request);

            // 2. Find the action method
            Method actionMethod = null;
            try {
                String controller = "controllers." + request.action.substring(0, request.action.lastIndexOf("."));
                String action = request.action.substring(request.action.lastIndexOf(".") + 1);
                Class controllerClass = Play.classloader.loadClass(controller);
                actionMethod = Java.findPublicStaticMethod(action, controllerClass);
            } catch (Exception e) {
                // ActionNotFound
                throw new RuntimeException(e);
            }

            if (actionMethod == null) {
                // ActionNotFound
                throw new RuntimeException("Not found");
            }
            
            // 3. Invoke the action
            try {
                actionMethod.invoke(null);
            } catch (IllegalAccessException ex) {
                // Nope
            } catch (IllegalArgumentException ex) {
                // ???
            } catch (InvocationTargetException ex) {                
                // It's a Result ? (expected)
                if(ex.getTargetException() instanceof Result) {
                    throw (Result)ex.getTargetException();
                }
                if(ex.getTargetException() instanceof RuntimeException) {
                    throw (RuntimeException)ex.getTargetException();
                }
                throw new RuntimeException(ex);
            }


        } catch (Result result) {
            result.apply(request, response);
            
        } finally {
            try {
                response.out.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    } 
}
