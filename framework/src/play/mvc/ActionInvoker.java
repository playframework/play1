package play.mvc;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import java.io.ByteArrayInputStream;
import play.mvc.results.Result;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import play.Play;
import play.PlayPlugin;
import play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation;
import play.classloading.enhancers.ControllersEnhancer.ControllerSupport;
import play.data.binding.Binder;
import play.data.parsing.UrlEncodedParser;
import play.data.validation.Validation;
import play.exceptions.JavaExecutionException;
import play.exceptions.ActionNotFoundException;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.i18n.Lang;
import play.utils.Java;
import play.mvc.results.NotFound;
import play.mvc.results.Ok;

/**
 * Invoke an action after an HTTP request
 */
public class ActionInvoker {

    public static void invoke(Http.Request request, Http.Response response) {
        Monitor monitor = null;
        try {
            if(!Play.started) {
                return;
            }
            
            Http.Request.current.set(request);
            Http.Response.current.set(response);
            
            Scope.Params.current.set(new Scope.Params());
            Scope.RenderArgs.current.set(new Scope.RenderArgs());
            Scope.Session.current.set(Scope.Session.restore());
            Scope.Flash.current.set(Scope.Flash.restore());

            // 1. Route and resolve format if not already done
            if(request.action == null) {
                for (PlayPlugin plugin : Play.plugins) {
                    plugin.routeRequest(request);
                }
                Router.route(request);
            }
            request.resolveFormat();

            // 2. Find the action method
            Method actionMethod = null;
            try {
                Object[] ca = getActionMethod(request.action);
                actionMethod = (Method) ca[1];
                request.controller = ((Class) ca[0]).getName().substring(12);
                request.actionMethod = actionMethod.getName();
                request.action = request.controller + "." + request.actionMethod;
                request.invokedMethod = actionMethod;
            } catch (ActionNotFoundException e) {
                throw new NotFound(String.format("%s action not found", e.getAction()));
            }

            // 3. Prepare request params
            Scope.Params.current().__mergeWith(request.routeArgs);
            // add parameters from the URI query string 
            Scope.Params.current()._mergeWith(UrlEncodedParser.parseQueryString(new ByteArrayInputStream(request.querystring.getBytes("utf-8"))));
            Lang.resolvefrom(request);

            // 4. Easy debugging ...
            if (Play.mode == Play.Mode.DEV) {
                Controller.class.getDeclaredField("params").set(null, Scope.Params.current());
                Controller.class.getDeclaredField("request").set(null, Http.Request.current());
                Controller.class.getDeclaredField("response").set(null, Http.Response.current());
                Controller.class.getDeclaredField("session").set(null, Scope.Session.current());
                Controller.class.getDeclaredField("flash").set(null, Scope.Flash.current());
                Controller.class.getDeclaredField("renderArgs").set(null, Scope.RenderArgs.current());
                Controller.class.getDeclaredField("validation").set(null, Java.invokeStatic(Validation.class, "current"));
            }
            
            for (PlayPlugin plugin : Play.plugins) {
                plugin.beforeActionInvocation(actionMethod);
            }

            // Monitoring
            monitor = MonitorFactory.start(request.action+"()");
            
            // 5. Invoke the action
            try {
                // @Before
                List<Method> befores = Java.findAllAnnotatedMethods(Controller.getControllerClass(), Before.class);
                ControllerInstrumentation.stopActionCall();
                for (Method before : befores) {
                    String[] unless = before.getAnnotation(Before.class).unless();
                    boolean skip = false;
                    for (String un : unless) {
                        if (!un.contains(".")) {
                            un = before.getDeclaringClass().getName().substring(12) + "." + un;
                        }
                        if (un.equals(request.action)) {
                            skip = true;
                            break;
                        }
                    }
                    if (!skip) {
                        if (Modifier.isStatic(before.getModifiers())) {
                            before.setAccessible(true);
                            Java.invokeStatic(before, getActionMethodArgs(before));
                        }
                    }
                }
                // Action
                Result actionResult = null;
                ControllerInstrumentation.initActionCall();
                try {
                    Java.invokeStatic(actionMethod, getActionMethodArgs(actionMethod));
                } catch (InvocationTargetException ex) {
                    // It's a Result ? (expected)
                    if (ex.getTargetException() instanceof Result) {
                        actionResult = (Result) ex.getTargetException();
                    } else {
                        throw ex;
                    }
                }
                
                // @After
                List<Method> afters = Java.findAllAnnotatedMethods(Controller.getControllerClass(), After.class);
                ControllerInstrumentation.stopActionCall();
                for (Method after : afters) {
                    String[] unless = after.getAnnotation(After.class).unless();
                    boolean skip = false;
                    for (String un : unless) {
                        if (!un.contains(".")) {
                            un = after.getDeclaringClass().getName().substring(12) + "." + un;
                        }
                        if (un.equals(request.action)) {
                            skip = true;
                            break;
                        }
                    }
                    if (!skip) {
                        if (Modifier.isStatic(after.getModifiers())) {
                            after.setAccessible(true);
                            Java.invokeStatic(after, getActionMethodArgs(after));
                        }
                    }
                }
                
                monitor.stop();
                monitor = null;
                
                // Ok, rethrow the original action result
                if(actionResult != null) {
                    throw actionResult;
                }
                
                throw new Ok();
                
            } catch (IllegalAccessException ex) {
                throw ex;
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (InvocationTargetException ex) {
                // It's a Result ? (expected)
                if (ex.getTargetException() instanceof Result) {
                    throw (Result) ex.getTargetException();
                }
                // Rethrow the enclosed exception
                if (ex.getTargetException() instanceof PlayException) {
                    throw (PlayException) ex.getTargetException();
                }
                StackTraceElement element = PlayException.getInterestingStrackTraceElement(ex.getTargetException());
                if (element != null) {
                    throw new JavaExecutionException(Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber(), ex.getTargetException());
                }
                throw new JavaExecutionException(Http.Request.current().action, ex);
            }


        } catch (Result result) {

            for (PlayPlugin plugin : Play.plugins) {
                plugin.onActionInvocationResult(result);
            }

            // Ok there is a result to apply
            // Save session & flash scope now

            Scope.Session.current().save();
            Scope.Flash.current().save();

            result.apply(request, response);

            for (PlayPlugin plugin : Play.plugins) {
                plugin.afterActionInvocation();
            }
            
            // @Finally
            if(Controller.getControllerClass() != null) {
                try {
                    List<Method> allFinally = Java.findAllAnnotatedMethods(Controller.getControllerClass(), Finally.class);
                    ControllerInstrumentation.stopActionCall();
                    for (Method aFinally : allFinally) {
                        String[] unless = aFinally.getAnnotation(Finally.class).unless();
                        boolean skip = false;
                        for (String un : unless) {
                            if (!un.contains(".")) {
                                un = aFinally.getDeclaringClass().getName().substring(12) + "." + un;
                            }
                            if (un.equals(request.action)) {
                                skip = true;
                                break;
                            }
                        }
                        if (!skip) {
                            if (Modifier.isStatic(aFinally.getModifiers())) {
                                aFinally.setAccessible(true);
                                Java.invokeStatic(aFinally, new Object[aFinally.getParameterTypes().length]);
                            }
                        }
                    }
                } catch(InvocationTargetException ex) {
                    StackTraceElement element = PlayException.getInterestingStrackTraceElement(ex.getTargetException());
                    if (element != null) {
                        throw new JavaExecutionException(Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber(), ex.getTargetException());
                    }
                    throw new JavaExecutionException(Http.Request.current().action, ex);
                } catch(Exception e) {
                    throw new UnexpectedException("Exception while doing @Finally", e);
                }
            }

        } catch (PlayException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        } finally {
            if(monitor != null) {
                monitor.stop();
            }
        }

    }

    public static Object[] getActionMethod(String fullAction) {
        Method actionMethod = null;
        Class controllerClass = null;
        try {
            if (!fullAction.startsWith("controllers.")) {
                fullAction = "controllers." + fullAction;
            }
            String controller = fullAction.substring(0, fullAction.lastIndexOf("."));
            String action = fullAction.substring(fullAction.lastIndexOf(".") + 1);
            controllerClass = Play.classloader.getClassIgnoreCase(controller);
            if(!ControllerSupport.class.isAssignableFrom(controllerClass)) {
                throw new ActionNotFoundException(fullAction, new Exception("class " + controller + " does not extend play.mvc.Controller"));
                
            }
            actionMethod = Java.findActionMethod(action, controllerClass);
            if (actionMethod == null) {
                throw new ActionNotFoundException(fullAction, new Exception("No method public static void " + action + "() was found in class " + controller));
            }
        } catch (PlayException e) {
            throw e;
        } catch (Exception e) {
            throw new ActionNotFoundException(fullAction, e);
        }
        return new Object[]{controllerClass, actionMethod};
    }
    
    public static Object[] getActionMethodArgs(Method method) throws Exception {
        String[] paramsNames = Java.parameterNames(method);      
        if (paramsNames == null && method.getParameterTypes().length > 0) {
            throw new UnexpectedException("Parameter names not found for method " + method);
        }
        Object[] rArgs = new Object[method.getParameterTypes().length];
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            Class type = method.getParameterTypes()[i];
            Map<String, String[]> params = new HashMap();
            if(type.equals(String.class) || Number.class.isAssignableFrom(type) || type.isPrimitive()) {
                params.put(paramsNames[i], Scope.Params.current().getAll(paramsNames[i]));
            } else {
                params.putAll(Scope.Params.current().all());
            }
            rArgs[i] = Binder.bind(paramsNames[i], method.getParameterTypes()[i], method.getGenericParameterTypes()[i], params);
        }
        return rArgs;
    }
}
