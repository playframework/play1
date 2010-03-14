package play.mvc;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.mvc.Router.Route;
import play.mvc.results.Result;
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
import play.libs.MimeTypes;
import play.mvc.results.NoResult;
import play.utils.Java;
import play.mvc.results.NotFound;
import play.mvc.results.RenderBinary;
import play.mvc.results.RenderText;
import play.utils.Utils;

/**
 * Invoke an action after an HTTP request.
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
                Route route = Router.route(request);
                for (PlayPlugin plugin : Play.plugins) {
                	plugin.onRequestRouting(route);
                }
            }
            request.resolveFormat();

            // 2. Find the action method
            Method actionMethod = null;
            try {
                Object[] ca = getActionMethod(request.action);
                actionMethod = (Method) ca[1];
                request.controller = ((Class) ca[0]).getName().substring(12).replace("$", "");
                request.controllerClass = ((Class) ca[0]);
                request.actionMethod = actionMethod.getName();
                request.action = request.controller + "." + request.actionMethod;
                request.invokedMethod = actionMethod;
            } catch (ActionNotFoundException e) {
                Logger.error(e, "%s action not found", e.getAction());
                throw new NotFound(String.format("%s action not found", e.getAction()));
            }
            
            Logger.trace("------- %s", actionMethod);

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
                Controller.class.getDeclaredField("validation").set(null, Validation.current());
            }

            ControllerInstrumentation.stopActionCall();
            for (PlayPlugin plugin : Play.plugins) {
                plugin.beforeActionInvocation(actionMethod);
            }

            // Monitoring
            monitor = MonitorFactory.start(request.action+"()");
            
            // 5. Invoke the action

            // There is a difference between a get and a post when binding data. The get does not care about validation while
            // the post do.
            try {
                // @Before
                List<Method> befores = Java.findAllAnnotatedMethods(Controller.getControllerClass(), Before.class);
                Collections.sort(befores, new Comparator<Method>() {

                    public int compare(Method m1, Method m2) {
                        Before before1 = m1.getAnnotation(Before.class);
                        Before before2 = m2.getAnnotation(Before.class);
                        return before1.priority() - before2.priority();
                    }
                });
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
                        before.setAccessible(true);
                        invokeControllerMethod(before, true);
                    }
                }
                // Action
                Result actionResult = null;
                ControllerInstrumentation.initActionCall();
                try {
                    Object o = invokeControllerMethod(actionMethod, true);
                    if(o != null) {
                        if(o instanceof InputStream) {
                            response.setContentTypeIfNotSet("application/octet-stream");
                            throw new RenderBinary((InputStream)o, null ,true);
                        }
                        if(o instanceof File) {
                            response.setContentTypeIfNotSet("application/octet-stream");
                            throw new RenderBinary((File)o);
                        }
                        response.setContentTypeIfNotSet(MimeTypes.getContentType("x."+request.format, "text/plain"));
                        throw new InvocationTargetException(new RenderText(o.toString()));
                    }
                } catch (InvocationTargetException ex) {
                    // It's a Result ? (expected)
                    if (ex.getTargetException() instanceof Result) {
                        actionResult = (Result) ex.getTargetException();
                    } else {
                        // @Catch
                        Object[] args = new Object[] { ex.getTargetException() };
                        List<Method> catches = Java.findAllAnnotatedMethods(Controller.getControllerClass(), Catch.class);
                        Collections.sort(catches, new Comparator<Method>() {

                            public int compare(Method m1, Method m2) {
                                Catch catch1 = m1.getAnnotation(Catch.class);
                                Catch catch2 = m2.getAnnotation(Catch.class);
                                return catch1.priority() - catch2.priority();
                            }
                        });
                        ControllerInstrumentation.stopActionCall();
                        for (Method mCatch : catches) {
                            Class[] exceptions = mCatch.getAnnotation(Catch.class).value();
                            for (Class exception : exceptions) {
                                if (exception.isInstance(args[0])) {
                                    mCatch.setAccessible(true);
                                    invokeControllerMethod(mCatch, false);
                                    break;
                                }
                            }
                        }

                        throw ex;
                    }
                }
                
                // @After
                List<Method> afters = Java.findAllAnnotatedMethods(Controller.getControllerClass(), After.class);
                Collections.sort(afters, new Comparator<Method>() {

                    public int compare(Method m1, Method m2) {
                        After after1 = m1.getAnnotation(After.class);
                        After after2 = m2.getAnnotation(After.class);
                        return after1.priority() - after2.priority();
                    }
                });
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
                        after.setAccessible(true);
                        invokeControllerMethod(after, true);
                    }
                }
                
                monitor.stop();
                monitor = null;
                
                // OK, re-throw the original action result
                if(actionResult != null) {
                    throw actionResult;
                }
                
                throw new NoResult();
                
            } catch (IllegalAccessException ex) {
                throw ex;
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (InvocationTargetException ex) {
                // It's a Result ? (expected)
                if (ex.getTargetException() instanceof Result) {
                    throw (Result) ex.getTargetException();
                }
                // Re-throw the enclosed exception
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

            // OK there is a result to apply
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
                    Collections.sort(allFinally, new Comparator<Method>() {

                        public int compare(Method m1, Method m2) {
                            Finally finally1 = m1.getAnnotation(Finally.class);
                            Finally finally2 = m2.getAnnotation(Finally.class);
                            return finally1.priority() - finally2.priority();
                        }
                    });
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
                            aFinally.setAccessible(true);
                            invokeControllerMethod(aFinally, false);
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

    public static Object invokeControllerMethod(Method method, boolean withBind) throws Exception {
        if(Modifier.isStatic(method.getModifiers()) && !method.getDeclaringClass().getName().matches("^controllers\\..*\\$class$")) {
            return method.invoke(null, getActionMethodArgs(method, null));
        } else if(Modifier.isStatic(method.getModifiers())) {
            Object[] args = getActionMethodArgs(method, null);
            args[0] = Http.Request.current().controllerClass.getDeclaredField("MODULE$").get(null);
            return method.invoke(null, args);
        } else {
            Object instance = null;
            try {
                instance = method.getDeclaringClass().getDeclaredField("MODULE$").get(null);
            } catch(Exception e) {
                throw new ActionNotFoundException(Http.Request.current().action, e);
            }
            return method.invoke(instance, getActionMethodArgs(method, instance));
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
            if(controllerClass == null) {
                throw new ActionNotFoundException(fullAction, new Exception("Controller " + controller + " not found"));
            }
            if(!ControllerSupport.class.isAssignableFrom(controllerClass)) {                
                // Try the scala way
                controllerClass = Play.classloader.getClassIgnoreCase(controller+"$");
                if(!ControllerSupport.class.isAssignableFrom(controllerClass)) {
                    throw new ActionNotFoundException(fullAction, new Exception("class " + controller + " does not extend play.mvc.Controller"));
                }
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
    
    public static Object[] getActionMethodArgs(Method method, Object o) throws Exception {
        String[] paramsNames = Java.parameterNames(method);      
        if (paramsNames == null && method.getParameterTypes().length > 0) {
            throw new UnexpectedException("Parameter names not found for method " + method);
        }
        Object[] rArgs = new Object[method.getParameterTypes().length];
        for (int i = 0; i < method.getParameterTypes().length; i++) {

            Class type = method.getParameterTypes()[i];
            Map<String, String[]> params = new HashMap<String, String[]>();
            if(type.equals(String.class) || Number.class.isAssignableFrom(type) || type.isPrimitive()) {
                params.put(paramsNames[i], Scope.Params.current().getAll(paramsNames[i]));
            } else {
                params.putAll(Scope.Params.current().all());
            }
            Logger.trace("getActionMethodArgs name [" + paramsNames[i] + "] annotation [" + Utils.toString(method.getParameterAnnotations()[i]) + "]");

            rArgs[i] = Binder.bind(paramsNames[i], method.getParameterTypes()[i], method.getGenericParameterTypes()[i], method.getParameterAnnotations()[i], params, o, method, i+1);
        }
        return rArgs;
    }


}
