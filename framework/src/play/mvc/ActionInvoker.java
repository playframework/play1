package play.mvc;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import org.apache.commons.javaflow.Continuation;
import org.apache.commons.javaflow.bytecode.StackRecorder;
import play.Invoker.Suspend;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.cache.CacheFor;
import play.classloading.enhancers.ControllersEnhancer;
import play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation;
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
import play.inject.Injector;
import play.mvc.Http.Request;
import play.mvc.Router.Route;
import play.mvc.results.NoResult;
import play.mvc.results.NotFound;
import play.mvc.results.Result;
import play.utils.Java;
import play.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Future;

/**
 * Invoke an action after an HTTP request.
 */
public class ActionInvoker {

    @SuppressWarnings("unchecked")
    public static void resolve(Http.Request request) {

        if (!Play.started) {
            return;
        }

        if (request.resolved) {
            return;
        }

        initActionContext(request, Http.Response.current.get());

        // Route and resolve format if not already done
        if (request.action == null) {
            Play.pluginCollection.routeRequest(request);
            Route route = Router.route(request);
            Play.pluginCollection.onRequestRouting(route);
        }
        request.resolveFormat();

        // Find the action method
        try {
            Method actionMethod;
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

    private static void initActionContext(Http.Request request, Http.Response response) {
        Http.Request.current.set(request);
        Http.Response.current.set(response);

        Scope.Params.current.set(request.params);
        Scope.RenderArgs.current.set(new Scope.RenderArgs());
        Scope.RouteArgs.current.set(new Scope.RouteArgs());
        Scope.Session.current.set(Scope.Session.restore());
        Scope.Flash.current.set(Scope.Flash.restore());
        CachedBoundActionMethodArgs.init();

        ControllersEnhancer.currentAction.set(new Stack<>());
    }

    public static void invoke(Http.Request request, Http.Response response) {
        Monitor monitor = null;

        try {
            initActionContext(request, response);
            Method actionMethod = request.invokedMethod;

            // 1. Prepare request params
            Scope.Params.current().__mergeWith(request.routeArgs);

            // add parameters from the URI query string
            String encoding = Http.Request.current().encoding;
            Scope.Params.current()
                    ._mergeWith(UrlEncodedParser.parseQueryString(new ByteArrayInputStream(request.querystring.getBytes(encoding))));

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

            String cacheKey = null;
            Result actionResult = null;

            // 3. Invoke the action
            try {
                // @Before
                handleBefores(request);

                // Action

                // Check the cache (only for GET or HEAD)
                if ((request.method.equals("GET") || request.method.equals("HEAD")) && actionMethod.isAnnotationPresent(CacheFor.class)) {
                    CacheFor cacheFor = actionMethod.getAnnotation(CacheFor.class);;
                    cacheKey = cacheFor.id();
                    if ("".equals(cacheKey)) {
                        // Generate a cache key for this request
                        cacheKey = cacheFor.generator().newInstance().generate(request);
                    }
                    if(cacheKey != null && !"".equals(cacheKey)) {
                    	actionResult = (Result) Cache.get(cacheKey);
                    }
                }

                if (actionResult == null) {
                    ControllerInstrumentation.initActionCall();
                    inferResult(invokeControllerMethod(actionMethod));
                }
            } catch (Result result) {
                actionResult = result;
                // Cache it if needed
                if (cacheKey != null && !"".equals(cacheKey)) {
                    Cache.set(cacheKey, actionResult, actionMethod.getAnnotation(CacheFor.class).value());
                }
            } catch (JavaExecutionException e) {
                invokeControllerCatchMethods(e.getCause());
                throw e;
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

        } catch (JavaExecutionException e) {
            handleFinallies(request, e.getCause());
            throw e;
        } catch (PlayException e) {
            handleFinallies(request, e);
            throw e;
        } catch (Throwable e) {
            handleFinallies(request, e);
            throw new UnexpectedException(e);
        } finally {
            Play.pluginCollection.onActionInvocationFinally();

            if (monitor != null) {
                monitor.stop();
            }
        }
    }

    private static void invokeControllerCatchMethods(Throwable throwable) throws Exception {
        // @Catch
        Object[] args = new Object[] {throwable};
        List<Method> catches = Java.findAllAnnotatedMethods(getControllerClass(), Catch.class);
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
    }

    private static boolean isActionMethod(Method method) {
        return !method.isAnnotationPresent(Before.class) &&
                !method.isAnnotationPresent(After.class) &&
                !method.isAnnotationPresent(Finally.class) &&
                !method.isAnnotationPresent(Catch.class) &&
                !method.isAnnotationPresent(Util.class);
    }

    /**
     * Find the first public method of a controller class
     *
     * @param name
     *            The method name
     * @param clazz
     *            The class
     * @return The method or null
     */
    public static Method findActionMethod(String name, Class clazz) {
        while (!clazz.getName().equals("java.lang.Object")) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equalsIgnoreCase(name) && Modifier.isPublic(m.getModifiers())) {
                    // Check that it is not an interceptor
                    if (isActionMethod(m)) {
                        return m;
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static void handleBefores(Http.Request request) throws Exception {
        List<Method> befores = Java.findAllAnnotatedMethods(getControllerClass(), Before.class);
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
        List<Method> afters = Java.findAllAnnotatedMethods(getControllerClass(), After.class);
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
     * Checks and calla all methods in controller annotated with @Finally. The
     * caughtException-value is sent as argument to @Finally-method if method
     * has one argument which is Throwable
     *
     * @param request
     * @param caughtException
     *            If @Finally-methods are called after an error, this variable
     *            holds the caught error
     * @throws PlayException
     */
    static void handleFinallies(Http.Request request, Throwable caughtException) throws PlayException {

        if (getControllerClass() == null) {
            // skip it
            return;
        }

        try {
            List<Method> allFinally = Java.findAllAnnotatedMethods(Request.current().controllerClass, Finally.class);
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

                    // check if method accepts Throwable as only parameter
                    Class[] parameterTypes = aFinally.getParameterTypes();
                    if (parameterTypes.length == 1 && parameterTypes[0] == Throwable.class) {
                        // invoking @Finally method with caughtException as
                        // parameter
                        invokeControllerMethod(aFinally, new Object[] { caughtException });
                    } else {
                        // invoke @Finally-method the regular way without
                        // caughtException
                        invokeControllerMethod(aFinally, null);
                    }
                }
            }
        } catch (PlayException e) {
            throw e;
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
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        String declaringClassName = method.getDeclaringClass().getName();
        boolean isProbablyScala = declaringClassName.contains("$");

        Http.Request request = Http.Request.current();

        if (!isStatic && request.controllerInstance == null) {
            request.controllerInstance = Injector.getBeanOfType(request.controllerClass);
        }

        Object[] args = forceArgs != null ? forceArgs : getActionMethodArgs(method, request.controllerInstance);

        if (isProbablyScala) {
            try {
                Object scalaInstance = request.controllerClass.getDeclaredField("MODULE$").get(null);
                if (declaringClassName.endsWith("$class")) {
                    args[0] = scalaInstance; // Scala trait method
                } else {
                    request.controllerInstance = (PlayController) scalaInstance; // Scala object method
                }
            } catch (NoSuchFieldException e) {
                // not Scala
            }
        }

        Object methodClassInstance = isStatic ? null :
            (method.getDeclaringClass().isAssignableFrom(request.controllerClass)) ? request.controllerInstance :
                Injector.getBeanOfType(method.getDeclaringClass());

        return invoke(method, methodClassInstance, args);
    }

    static Object invoke(Method method, Object instance, Object ... realArgs) throws Exception {
        try {
            if (isActionMethod(method)) {
                return invokeWithContinuation(method, instance, realArgs);
            } else {
                return method.invoke(instance, realArgs);
            }
        } catch (InvocationTargetException ex) {
            Throwable originalThrowable = ex.getTargetException();

            if (originalThrowable instanceof Result || originalThrowable instanceof PlayException)
                throw (Exception) originalThrowable;

            StackTraceElement element = PlayException.getInterestingStackTraceElement(originalThrowable);
            if (element != null) {
                throw new JavaExecutionException(Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber(),
                        originalThrowable);
            }
            throw new JavaExecutionException(originalThrowable);
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
        if (Request.current().args.containsKey(A)) {

            // Action0
            instance = Request.current().args.get(A);
            Future f = (Future) Request.current().args.get(F);
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
        Continuation continuation = (Continuation) Request.current().args.get(C);
        if (continuation == null) {
            continuation = new Continuation(new StackRecorder((Runnable) null));
        }

        StackRecorder pStackRecorder = new StackRecorder(continuation.stackRecorder);
        Object result = null;

        StackRecorder old = pStackRecorder.registerThread();
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
                Request.current().args.put(C, nextContinuation);

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
                Request.current().args.remove(C);
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
            if (!PlayController.class.isAssignableFrom(controllerClass)) {
                // Try the scala way
                controllerClass = Play.classloader.getClassIgnoreCase(controller + "$");
                if (!PlayController.class.isAssignableFrom(controllerClass)) {
                    throw new ActionNotFoundException(fullAction,
                            new Exception("class " + controller + " does not extend play.mvc.Controller"));
                }
            }
            actionMethod = findActionMethod(action, controllerClass);
            if (actionMethod == null) {
                throw new ActionNotFoundException(fullAction,
                        new Exception("No method public static void " + action + "() was found in class " + controller));
            }
        } catch (PlayException e) {
            throw e;
        } catch (Exception e) {
            throw new ActionNotFoundException(fullAction, e);
        }
        return new Object[] { controllerClass, actionMethod };
    }

    public static Object[] getActionMethodArgs(Method method, Object o) throws Exception {
        String[] paramsNames = Java.parameterNames(method);
        if (paramsNames == null && method.getParameterTypes().length > 0) {
            throw new UnexpectedException("Parameter names not found for method " + method);
        }

        // Check if we have already performed the bind operation
        Object[] rArgs = CachedBoundActionMethodArgs.current().retrieveActionMethodArgs(method);
        if (rArgs != null) {
            // We have already performed the binding-operation for this method
            // in this request.
            return rArgs;
        }

        rArgs = new Object[method.getParameterTypes().length];
        for (int i = 0; i < method.getParameterTypes().length; i++) {

            Class<?> type = method.getParameterTypes()[i];
            Map<String, String[]> params = new HashMap<>();

            // In case of simple params, we don't want to parse the body.
            if (type.equals(String.class) || Number.class.isAssignableFrom(type) || type.isPrimitive()) {
                params.put(paramsNames[i], Scope.Params.current().getAll(paramsNames[i]));
            } else {
                params.putAll(Scope.Params.current().all());
            }
            Logger.trace("getActionMethodArgs name [" + paramsNames[i] + "] annotation ["
                    + Utils.join(method.getParameterAnnotations()[i], " ") + "]");

            RootParamNode root = ParamNode.convert(params);
            rArgs[i] = Binder.bind(root, paramsNames[i], method.getParameterTypes()[i], method.getGenericParameterTypes()[i],
                    method.getParameterAnnotations()[i], new Binder.MethodAndParamInfo(o, method, i + 1));
        }

        CachedBoundActionMethodArgs.current().storeActionMethodArgs(method, rArgs);
        return rArgs;
    }

    private static Class<? extends PlayController> getControllerClass() {
        return Http.Request.current().controllerClass;
    }
}
