package play.mvc;

import java.io.ByteArrayInputStream;
import play.mvc.results.Result;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import play.Play;
import play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation;
import play.data.parsing.DataParser;
import play.libs.Java;

public class ActionInvoker {

    public static void invoke(Http.Request request, Http.Response response) {
        try {
            
            Http.Request.current.set(request);
            Http.Response.current.set(response);

            // 1. Route the request
            Router.route(request);

            // 2. Find the action method
            Method actionMethod = getActionMethod(request.action);
            
            // 3. Prepare request params
            Scope.Params params = new Scope.Params();
            Scope.Params.current.set(params);
            params.__mergeWith(request.routeArgs);
            params._mergeWith(DataParser.parsers.get("application/x-www-form-urlencoded").parse(new ByteArrayInputStream(request.querystring.getBytes("utf-8"))));
            
            // 4. Invoke the action
            try {
                // @Before
                List<Method> befores = Java.findAllAnnotatedMethods(actionMethod.getDeclaringClass(), Before.class);
                ControllerInstrumentation.stopActionCall();
                for(Method before : befores) {
                    if(Modifier.isStatic(before.getModifiers())) {
                        before.setAccessible(true);
                        if(before.getParameterTypes().length>0) {
                            params.checkAndParse();
                        }
                        Java.invokeStatic(before, params.data);
                    }
                }
                // Action
                ControllerInstrumentation.initActionCall();
                if(actionMethod.getParameterTypes().length>0) {
                    params.checkAndParse();
                }
                Java.invokeStatic(actionMethod, params.data);
            } catch (IllegalAccessException ex) {
                throw ex;
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (InvocationTargetException ex) {                
                // It's a Result ? (expected)
                if(ex.getTargetException() instanceof Result) {
                    throw (Result)ex.getTargetException();
                }
                // Rethrow the enclosed exception
                if(ex.getTargetException() instanceof RuntimeException) {
                    throw (RuntimeException)ex.getTargetException();
                }
                throw new RuntimeException(ex.getTargetException());
            }


        } catch (Result result) {
            result.apply(request, response);
            
        } catch (Exception e) {
             throw new RuntimeException(e);
        } 
        
    } 
    
    public static Method getActionMethod(String fullAction) {
        Method actionMethod = null;
        try {
            if(!fullAction.startsWith("controllers.")) {
                fullAction = "controllers." + fullAction;
            }
            String controller = fullAction.substring(0, fullAction.lastIndexOf("."));
            String action = fullAction.substring(fullAction.lastIndexOf(".") + 1);
            Class controllerClass = Play.classloader.loadClass(controller);
            actionMethod = Java.findPublicStaticMethod(action, controllerClass);
        } catch (Exception e) {
            // ActionNotFound
            throw new RuntimeException("Not found");
        }
        if (actionMethod == null) {
            // ActionNotFound
            throw new RuntimeException("Not found");
        }
        return actionMethod;
    }
}
