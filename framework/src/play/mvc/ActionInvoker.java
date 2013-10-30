package play.mvc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import play.Logger;
import play.Play;
import play.cache.CacheFor;
import play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation;
import play.classloading.enhancers.ControllersEnhancer.ControllerSupport;
import play.data.binding.Binder;
import play.data.binding.CachedBoundActionMethodArgs;
import play.data.binding.ParamNode;
import play.data.binding.RootParamNode;
import play.data.parsing.UrlEncodedParser;
import play.data.validation.Validation;
import play.exceptions.ActionNotFoundException;
import play.exceptions.JavaExecutionException;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.i18n.Lang;
import play.mvc.Http.Request;
import play.mvc.Router.Route;
import play.mvc.results.NoResult;
import play.mvc.results.Result;
import play.utils.Java;
import play.utils.Utils;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import java.util.concurrent.Future;
import org.apache.commons.javaflow.Continuation;
import org.apache.commons.javaflow.bytecode.StackRecorder;
import play.Invoker.Suspend;
import play.classloading.enhancers.ControllersEnhancer;
import play.mvc.results.NotFound;

/**
 * Invoke an action after an HTTP request.
 */
public class ActionInvoker {

    @SuppressWarnings("unchecked")
    public static void resolve(Http.Request request, Http.Response response) {

        if (!Play.started) {
            return;
        }

        Http.Request.current.set(request);
        Http.Response.current.set(response);

        Scope.Params.current.set(request.params);
        Scope.RenderArgs.current.set(new Scope.RenderArgs());
        Scope.RouteArgs.current.set(new Scope.RouteArgs());
        Scope.Session.current.set(Scope.Session.restore());
        Scope.Flash.current.set(Scope.Flash.restore());
        CachedBoundActionMethodArgs.init();

        ControllersEnhancer.currentAction.set(new Stack<String>());

        if (request.resolved) {
            return;
        }

        // Route and resolve format if not already done
        if (request.action == null) {
            Play.pluginCollection.routeRequest(request);
            Route route = Router.route(request);
            Play.pluginCollection.onRequestRouting(route);
        }
        request.resolveFormat();

        // Find the action method
        try {
            Method actionMethod = null;
            Object[] ca = getActionMethod(request.action);
            actionMethod = (Method) ca[1];
            request.controller = ((Class) ca[0]).getName().substring(12).replace("$", "");
            request.controllerClass = ((Class) ca[0]);
            request.actionMethod = actionMethod.getName();
            request.action = request.controller + "." + request.actionMethod;
            request.invokedMethod = actionMethod;

            if (Logger.isTraceEnabled()) {
                Logger.trace("------- %s", actionMethod);
            }

            request.resolved = true;

        } catch (ActionNotFoundException e) {
            Logger.error(e, "%s action not found", e.getAction());
            throw new NotFound(String.format("%s action not found", e.getAction()));
        }

    }

    public static void invoke(Http.Request request, Http.Response response) {
        Monitor monitor = null;

        try {

            resolve(request, response);
            Method actionMethod = request.invokedMethod;

            // 1. Prepare request params
            Scope.Params.current().__mergeWith(request.routeArgs);

            // add parameters from the URI query string
            String encoding = Http.Request.current().encoding;
            Scope.Params.current()._mergeWith(UrlEncodedParser.parseQueryString(new ByteArrayInputStream(request.querystring.getBytes(encoding))));

            // 2. Easy debugging ...
            if (Play.mode == Play.Mode.DEV) {
                Controller.class.getDeclaredField("params").set(null, Scope.Params.current());
                Controller.class.getDeclaredField("request").set(null, Http.Request.current());
                Controller.class.getDeclaredField("response").set(null, Http.Response.current());
                Controller.class.getDeclaredField("session").set(null, Scope.Session.current());
                Controller.class.getDeclaredField("flash").set(null, Scope.Flash.current());
                Controller.class.getDeclaredField("renderArgs").set(null, Scope.RenderArgs.current());
                Controller.class.getDeclaredField("routeArgs").set(null, Scope.RouteArgs.current());
                Controller.class.getDeclaredField("validation").set(null, Validation.current());
            }

            ControllerInstrumentation.stopActionCall();
            Play.pluginCollection.beforeActionInvocation(actionMethod);

            // Monitoring
            monitor = MonitorFactory.start(request.action + "()");

            // 3. Invoke the action
            try {
                // @Before
                handleBefores(request);

                // Action

                Result actionResult = null;
                String cacheKey = null;

                // Check the cache (only for GET or HEAD)
                if ((request.method.equals("GET") || request.method.equals("HEAD")) && actionMethod.isAnnotationPresent(CacheFor.class)) {
                    cacheKey = actionMethod.getAnnotation(CacheFor.class).id();
                    if ("".equals(cacheKey)) {
                        cacheKey = "urlcache:" + request.url + request.querystring;
                    }
                    actionResult = (Result) play.cache.Cache.get(cacheKey);
                }

                if (actionResult == null) {
                    ControllerInstrumentation.initActionCall();
                    try {
                        inferResult(invokeControllerMethod(actionMethod));
                    } catch(Result result) {
                        actionResult = result;
                        // Cache it if needed
                        if (cacheKey != null) {
                            play.cache.Cache.set(cacheKey, actionResult, actionMethod.getAnnotation(CacheFor.class).value());
                        }
                    } catch (InvocationTargetException ex) {
                        // It's a Result ? (expected)
                        if (ex.getTargetException() instanceof Result) {
                            actionResult = (Result) ex.getTargetException();
                            // Cache it if needed
                            if (cacheKey != null) {
                                play.cache.Cache.set(cacheKey, actionResult, actionMethod.getAnnotation(CacheFor.class).value());
                            }

                        } else {
                            // @Catch
                            Object[] args = new Object[]{ex.getTargetException()};
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
                                if (exceptions.length == 0) {
                                    exceptions = new Class[]{Exception.class};
                                }
                                for (Class exception : exceptions) {
                                    if (exception.isInstance(args[0])) {
                                        mCatch.setAccessible(true);
                                        inferResult(invokeControllerMethod(mCatch, args));
                                        break;
                                    }
                                }
                            }

                            throw ex;
                        }
                    }
                }

                // @After
                handleAfters(request);

                monitor.stop();
                monitor = null;

                // OK, re-throw the original action result
                if (actionResult != null) {
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

            Play.pluginCollection.onActionInvocationResult(result);

            // OK there is a result to apply
            // Save session & flash scope now

            Scope.Session.current().save();
            Scope.Flash.current().save();

            result.apply(request, response);

            Play.pluginCollection.afterActionInvocation();

            // @Finally
            handleFinallies(request, null);

        } catch (PlayException e) {
            handleFinallies(request, e);
            throw e;
        } catch (Throwable e) {
            handleFinallies(request, e);
            throw new UnexpectedException(e);
        } finally {
            if (monitor != null) {
                monitor.stop();
            }
        }
    }

    private static boolean isActionMethod(Method method) {
        if (method.isAnnotationPresent(Before.class)) {
            return false;
        }
        if (method.isAnnotationPresent(After.class)) {
            return false;
        }
        if (method.isAnnotationPresent(Finally.class)) {
            return false;
        }
        if (method.isAnnotationPresent(Catch.class)) {
            return false;
        }
        if (method.isAnnotationPresent(Util.class)) {
            return false;
        }
        return true;
    }

    private static void handleBefores(Http.Request request) throws Exception {
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
            String[] only = before.getAnnotation(Before.class).only();
            boolean skip = false;
            for (String un : only) {
                if (!un.contains(".")) {
                    un = before.getDeclaringClass().getName().substring(12).replace("$", "") + "." + un;
                }
                if (un.equals(request.action)) {
                    skip = false;
                    break;
                } else {
                    skip = true;
                }
            }
            for (String un : unless) {
                if (!un.contains(".")) {
                    un = before.getDeclaringClass().getName().substring(12).replace("$", "") + "." + un;
                }
                if (un.equals(request.action)) {
                    skip = true;
                    break;
                }
            }
            if (!skip) {
                before.setAccessible(true);
                inferResult(invokeControllerMethod(before));
            }
        }
    }

    private static void handleAfters(Http.Request request) throws Exception {
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
            String[] only = after.getAnnotation(After.class).only();
            boolean skip = false;
            for (String un : only) {
                if (!un.contains(".")) {
                    un = after.getDeclaringClass().getName().substring(12) + "." + un;
                }
                if (un.equals(request.action)) {
                    skip = false;
                    break;
                } else {
                    skip = true;
                }
            }
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
                inferResult(invokeControllerMethod(after));
            }
        }
    }

    /**
     * Checks and calla all methods in controller annotated with @Finally.
     * The caughtException-value is sent as argument to @Finally-method if method has one argument which is Throwable
     * @param request
     * @param caughtException If @Finally-methods are called after an error, this variable holds the caught error
     * @throws PlayException
     */
    static void handleFinallies(Http.Request request, Throwable caughtException) throws PlayException {

        if (Controller.getControllerClass() == null) {
            //skip it
            return;
        }

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
                String[] only = aFinally.getAnnotation(Finally.class).only();
                boolean skip = false;
                for (String un : only) {
                    if (!un.contains(".")) {
                        un = aFinally.getDeclaringClass().getName().substring(12) + "." + un;
                    }
                    if (un.equals(request.action)) {
                        skip = false;
                        break;
                    } else {
                        skip = true;
                    }
                }
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

                    //check if method accepts Throwable as only parameter
                    Class[] parameterTypes = aFinally.getParameterTypes();
                    if (parameterTypes.length == 1 && parameterTypes[0] == Throwable.class) {
                        //invoking @Finally method with caughtException as parameter
                        invokeControllerMethod(aFinally, new Object[]{caughtException});
                    } else {
                        //invoce @Finally-method the regular way without caughtException
                        invokeControllerMethod(aFinally, null);
                    }
                }
            }
        } catch (InvocationTargetException ex) {
            StackTraceElement element = PlayException.getInterestingStrackTraceElement(ex.getTargetException());
            if (element != null) {
                throw new JavaExecutionException(Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber(), ex.getTargetException());
            }
            throw new JavaExecutionException(Http.Request.current().action, ex);
        } catch (Exception e) {
            throw new UnexpectedException("Exception while doing @Finally", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void inferResult(Object o) {
        // Return type inference
        if (o != null) {

            if (o instanceof NoResult) {
                return;
            }
            if (o instanceof Result) {
                // Of course
                throw (Result) o;
            }
            if (o instanceof InputStream) {
                Controller.renderBinary((InputStream) o);
            }
            if (o instanceof File) {
                Controller.renderBinary((File) o);
            }
            if (o instanceof Map) {
                Controller.renderTemplate((Map<String, Object>) o);
            }
            if (o instanceof Object[]) {
                Controller.render(o);
            }

            Controller.renderHtml(o);
        }
    }

    public static Object invokeControllerMethod(Method method) throws Exception {
        return invokeControllerMethod(method, null);
    }

    public static Object invokeControllerMethod(Method method, Object[] forceArgs) throws Exception {
        if (Modifier.isStatic(method.getModifiers()) && !method.getDeclaringClass().getName().matches("^controllers\\..*\\$class$")) {
            return invoke(method, null, forceArgs == null ? getActionMethodArgs(method, null) : forceArgs);
        } else if (Modifier.isStatic(method.getModifiers())) {
            Object[] args = getActionMethodArgs(method, null);
            args[0] = Http.Request.current().controllerClass.getDeclaredField("MODULE$").get(null);
            return invoke(method, null, args);
        } else {
            Object instance = null;
            try {
                instance = method.getDeclaringClass().getDeclaredField("MODULE$").get(null);
            } catch (Exception e) {
                Annotation[] annotations = method.getDeclaredAnnotations();
                String annotation = Utils.getSimpleNames(annotations);
                if (!StringUtils.isEmpty(annotation)) {
                    throw new UnexpectedException("Method public static void " + method.getName() + "() annotated with " + annotation + " in class " + method.getDeclaringClass().getName() + " is not static.");
                }
                // TODO: Find a better error report
                throw new ActionNotFoundException(Http.Request.current().action, e);
            }
            return invoke(method, instance, forceArgs == null ? getActionMethodArgs(method, instance) : forceArgs);
        }
    }

    static Object invoke(Method method, Object instance, Object[] realArgs) throws Exception {
        if(isActionMethod(method)) {
            return invokeWithContinuation(method, instance, realArgs);
        } else {
            return method.invoke(instance, realArgs);
        }
    }
    static final String C = "__continuation";
    static final String A = "__callback";
    static final String F = "__future";
    static final String CONTINUATIONS_STORE_LOCAL_VARIABLE_NAMES = "__CONTINUATIONS_STORE_LOCAL_VARIABLE_NAMES";
    static final String CONTINUATIONS_STORE_RENDER_ARGS = "__CONTINUATIONS_STORE_RENDER_ARGS";
    static final String CONTINUATIONS_STORE_PARAMS = "__CONTINUATIONS_STORE_PARAMS";
    public static final String CONTINUATIONS_STORE_VALIDATIONS = "__CONTINUATIONS_STORE_VALIDATIONS";
    static final String CONTINUATIONS_STORE_VALIDATIONPLUGIN_KEYS = "__CONTINUATIONS_STORE_VALIDATIONPLUGIN_KEYS";

    static Object invokeWithContinuation(Method method, Object instance, Object[] realArgs) throws Exception {
        // Callback case
        if (Http.Request.current().args.containsKey(A)) {

            // Action0
            instance = Http.Request.current().args.get(A);
            Future f = (Future) Http.Request.current().args.get(F);
            Scope.RenderArgs renderArgs = (Scope.RenderArgs) Request.current().args.remove(ActionInvoker.CONTINUATIONS_STORE_RENDER_ARGS);
            Scope.RenderArgs.current.set(renderArgs);
            if (f == null) {
                method = instance.getClass().getDeclaredMethod("invoke");
                method.setAccessible(true);
                return method.invoke(instance);
            } else {
                method = instance.getClass().getDeclaredMethod("invoke", Object.class);
                method.setAccessible(true);
                return method.invoke(instance, f.get());
            }

        }

        // Continuations case
        Continuation continuation = (Continuation) Http.Request.current().args.get(C);
        if (continuation == null) {
            continuation = new Continuation(new StackRecorder((Runnable) null));
        }

        StackRecorder pStackRecorder = new StackRecorder(continuation.stackRecorder);
        Object result = null;

        final StackRecorder old = pStackRecorder.registerThread();
        try {
            pStackRecorder.isRestoring = !pStackRecorder.isEmpty();

            // Execute code
            result = method.invoke(instance, realArgs);

            if (pStackRecorder.isCapturing) {
                if (pStackRecorder.isEmpty()) {
                    throw new IllegalStateException("stack corruption. Is " + method + " instrumented for javaflow?");
                }
                Object trigger = pStackRecorder.value;
                Continuation nextContinuation = new Continuation(pStackRecorder);
                Http.Request.current().args.put(C, nextContinuation);

                if (trigger instanceof Long) {
                    throw new Suspend((Long) trigger);
                }
                if (trigger instanceof Integer) {
                    throw new Suspend(((Integer) trigger).longValue());
                }
                if (trigger instanceof Future) {
                    throw new Suspend((Future) trigger);
                }

                throw new UnexpectedException("Unexpected continuation trigger -> " + trigger);
            } else {
                Http.Request.current().args.remove(C);
            }
        } finally {
            pStackRecorder.deregisterThread(old);
        }

        return result;
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
            if (controllerClass == null) {
                throw new ActionNotFoundException(fullAction, new Exception("Controller " + controller + " not found"));
            }
            if (!ControllerSupport.class.isAssignableFrom(controllerClass)) {
                // Try the scala way
                controllerClass = Play.classloader.getClassIgnoreCase(controller + "$");
                if (!ControllerSupport.class.isAssignableFrom(controllerClass)) {
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


        // Check if we have already performed the bind operation
        Object[] rArgs = CachedBoundActionMethodArgs.current().retrieveActionMethodArgs(method);
        if ( rArgs != null) {
            // We have already performed the binding-operation for this method
            // in this request.
            return rArgs;
        }

        rArgs = new Object[method.getParameterTypes().length];
        for (int i = 0; i < method.getParameterTypes().length; i++) {

            Class<?> type = method.getParameterTypes()[i];
            Map<String, String[]> params = new HashMap<String, String[]> ();

            // In case of simple params, we don't want to parse the body.
            if (type.equals(String.class) || Number.class.isAssignableFrom(type) || type.isPrimitive()) {
                params.put(paramsNames[i], Scope.Params.current().getAll(paramsNames[i]));
            } else {
                params.putAll(Scope.Params.current().all());
            }
            Logger.trace("getActionMethodArgs name [" + paramsNames[i] + "] annotation [" + Utils.join(method.getParameterAnnotations()[i], " ") + "]");

            RootParamNode root = ParamNode.convert(params);
            rArgs[i] = Binder.bind(
                        root,
                        paramsNames[i],
                        method.getParameterTypes()[i],
                        method.getGenericParameterTypes()[i],
                        method.getParameterAnnotations()[i],
                        new Binder.MethodAndParamInfo(o, method, i + 1));
        }

        CachedBoundActionMethodArgs.current().storeActionMethodArgs(method, rArgs);
        return rArgs;
    }

}
