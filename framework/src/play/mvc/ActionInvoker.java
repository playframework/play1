package play.mvc;

import java.io.ByteArrayInputStream;
import play.mvc.results.Result;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import play.Play;
import play.PlayPlugin;
import play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation;
import play.data.parsing.DataParser;
import play.exceptions.JavaExecutionException;
import play.exceptions.ActionNotFoundException;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.i18n.Lang;
import play.libs.Java;

public class ActionInvoker {

    public static void invoke(Http.Request request, Http.Response response) {
        try {
            Http.Request.current.set(request);
            Http.Response.current.set(response);

            Scope.Params.current.set(new Scope.Params());
            Scope.RenderArgs.current.set(new Scope.RenderArgs());
            Scope.Session.current.set(Scope.Session.restore());
            Scope.Flash.current.set(Scope.Flash.restore());
            
            // 1. Route the request
            Router.route(request);

            // 2. Find the action method
            Method actionMethod = getActionMethod(request.action);  
            request.controller = actionMethod.getDeclaringClass().getName().substring(12);
            request.actionMethod = actionMethod.getName();
            
            // 3. Prepare request params
            Scope.Params.current().__mergeWith(request.routeArgs);
            // add parameters from the URI query string 
            Scope.Params.current()._mergeWith(DataParser.parsers.get("application/x-www-form-urlencoded").parse(new ByteArrayInputStream(request.querystring.getBytes("utf-8"))));
            Lang.resolvefrom(request);
            
            // 4. Easy debugging ...
            if(Play.mode == Play.Mode.DEV) {
                Controller.class.getField("params").set(null, Scope.Params.current());
                Controller.class.getField("request").set(null, Http.Request.current());
                Controller.class.getField("response").set(null, Http.Response.current());
                Controller.class.getField("session").set(null, Scope.Session.current());
                Controller.class.getField("flash").set(null, Scope.Flash.current());
                Controller.class.getField("renderArgs").set(null, Scope.RenderArgs.current());
            }
            
            for(PlayPlugin plugin : Play.plugins) {
                plugin.beforeActionInvocation();
            }
            
            // 5. Invoke the action
            try {
                // @Before
                List<Method> befores = Java.findAllAnnotatedMethods(actionMethod.getDeclaringClass(), Before.class);
                ControllerInstrumentation.stopActionCall();
                for(Method before : befores) {
                    String[] unless = before.getAnnotation(Before.class).unless();
                    boolean skip = false;
                    for(String un : unless) {
                        if(!un.contains(".")) {
                           un = before.getDeclaringClass().getName().substring(12) + "." + un; 
                        }
                        if(un.equals(request.action)) {
                            skip = true;
                            break;
                        }
                    }
                    if(!skip) {
                        if(Modifier.isStatic(before.getModifiers())) {
                            before.setAccessible(true);
                            if(before.getParameterTypes().length>0) {
                                Scope.Params.current().checkAndParse();
                            }
                            Java.invokeStatic(before, Scope.Params.current().all());
                        }
                    }
                }
                // Action
                ControllerInstrumentation.initActionCall();
                if(actionMethod.getParameterTypes().length>0) {
                    Scope.Params.current().checkAndParse();
                }
                Java.invokeStatic(actionMethod, Scope.Params.current().all());
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
                if(ex.getTargetException() instanceof PlayException) {
                    throw (PlayException)ex.getTargetException();
                }
                StackTraceElement element = PlayException.getInterestingStrackTraceElement(ex.getTargetException());
                if(element != null) {
                    throw new JavaExecutionException(Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber(), ex.getTargetException());
                }
                throw new JavaExecutionException(Http.Request.current().action, ex);
            }


        } catch (Result result) {
            
            for(PlayPlugin plugin : Play.plugins) {
                plugin.onActionInvocationResult(result);
            }
            
            // Ok there is a result to apply
            // Save session & flash scope now
        	
            Scope.Session.current().save();
            Scope.Flash.current().save();
            
            result.apply(request, response);
            
            for(PlayPlugin plugin : Play.plugins) {
                plugin.afterActionInvocation();
            }
            
        } catch(PlayException e) {
            throw e;
        } catch (Exception e) {
             throw new UnexpectedException(e);
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
            if (actionMethod == null) {
                throw new ActionNotFoundException(fullAction, new Exception("No method public static void "+action+"() was found in class "+controller));
            }
        } catch(PlayException e) {
            throw e;
        } catch (Exception e) {
            throw new ActionNotFoundException(fullAction, e);
        }        
        return actionMethod;
    }
}
