package play;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

import play.classloading.ApplicationClasses.ApplicationClass;
import play.data.binding.RootParamNode;
import play.db.Model;
import play.libs.F;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router.Route;
import play.mvc.results.Result;
import play.templates.BaseTemplate;
import play.templates.Template;
import play.test.BaseTest;
import play.test.TestEngine.TestResults;
import play.vfs.VirtualFile;

/**
 * A framework plugin
 */
public abstract class PlayPlugin implements Comparable<PlayPlugin> {

    /**
     * Plugin priority (0 for highest priority)
     */
    public int index;

    /**
     * Called at plugin loading
     */
    public void onLoad() {
    }

    public boolean compileSources() {
        return false;
    }

    /**
     * Run a test class
     * 
     * @param clazz
     *            the class to run
     * @return : tests results
     */
    public TestResults runTest(Class<BaseTest> clazz) {
        return null;
    }

    /**
     * Use method using RootParamNode instead
     * 
     * @param name
     *            the name of the object
     * @param clazz
     *            the class of the object to bind
     * @param type
     *            type
     * @param annotations
     *            annotation on the object
     * @param params
     *            parameters to bind
     * @return binding object
     * 
     * @deprecated use {@link #bind(RootParamNode, String, Class, Type, Annotation[])}
     */
    @Deprecated
    public Object bind(String name, Class clazz, Type type, Annotation[] annotations, Map<String, String[]> params) {
        return null;
    }

    /**
     * Called when play need to bind a Java object from HTTP params.
     *
     * When overriding this method, do not call super impl.. super impl is calling old bind method to be backward
     * compatible.
     * 
     * @param rootParamNode
     *            parameters to bind
     * @param name
     *            the name of the object
     * @param clazz
     *            the class of the object to bind
     * @param type
     *            type
     * @param annotations
     *            annotation on the object
     * @return binding object
     */
    public Object bind(RootParamNode rootParamNode, String name, Class<?> clazz, Type type, Annotation[] annotations) {
        // call old method to be backward compatible
        return bind(name, clazz, type, annotations, rootParamNode.originalParams);
    }

    /**
     * @deprecated Use bindBean instead
     * @param name
     *            the name of the object
     * @param o
     *            object to bind
     * @param params
     *            parameters to bind
     * @return binding object
     */
    @Deprecated
    public Object bind(String name, Object o, Map<String, String[]> params) {
        return null;
    }

    /**
     * Called when play need to bind an existing Java object from HTTP params. When overriding this method, DO NOT call
     * the super method, since its default impl is to call the old bind method to be backward compatible.
     * 
     * @param rootParamNode
     *            parameters to bind
     * @param name
     *            the name of the object
     * @param bean
     *            object to bind
     * @return binding object
     */
    public Object bindBean(RootParamNode rootParamNode, String name, Object bean) {
        // call old method to be backward compatible.
        return bind(name, bean, rootParamNode.originalParams);
    }

    /**
     * Unbind an object
     * 
     * @param src
     *            object to unbind
     * @param name
     *            the name of the object
     * @return List of parameters
     */
    public Map<String, Object> unBind(Object src, String name) {
        return null;
    }

    /**
     * Translate the given key for the given locale and arguments. If null is returned, Play's normal message
     * translation mechanism will be used.
     * 
     * @param locale
     *            the locale we want
     * @param key
     *            the message key
     * @param args
     *            arguments of the messages
     * @return the formatted string
     */
    public String getMessage(String locale, Object key, Object... args) {
        return null;
    }

    /**
     * Return the plugin status
     * 
     * @return the plugin status
     */
    public String getStatus() {
        return null;
    }

    /**
     * Return the plugin status in JSON format
     * 
     * @return the plugin status in JSON format
     */
    public JsonObject getJsonStatus() {
        return null;
    }

    /**
     * Enhance this class
     * 
     * @param applicationClass
     *            the class to enhance
     * @throws java.lang.Exception
     *             if cannot enhance the class
     */
    public void enhance(ApplicationClass applicationClass) throws Exception {
    }

    /**
     * This hook is not plugged, don't implement it
     * 
     * @param template
     *            the template to compile
     * @deprecated
     */
    @Deprecated
    public void onTemplateCompilation(Template template) {
    }

    /**
     * Give a chance to this plugin to fully manage this request
     * 
     * @param request
     *            The Play request
     * @param response
     *            The Play response
     * @return true if this plugin has managed this request
     * @throws java.lang.Exception
     *             if cannot enhance the class
     */
    public boolean rawInvocation(Request request, Response response) throws Exception {
        return false;
    }

    /**
     * Let a chance to this plugin to manage a static resource
     * 
     * @param file
     *            The requested file
     * @param request
     *            The Play request
     * @param response
     *            The Play response
     * @return true if this plugin has managed this request
     */
    public boolean serveStatic(VirtualFile file, Request request, Response response) {
        return false;
    }

    public void beforeDetectingChanges() {
    }

    /**
     * @param file
     *            the file of the template to load
     * @return the template object
     */
    public Template loadTemplate(VirtualFile file) {
        return null;
    }

    /**
     * It's time for the plugin to detect changes. Throw an exception is the application must be reloaded.
     */
    public void detectChange() {
    }

    /**
     * It's time for the plugin to detect changes. Throw an exception is the application must be reloaded.
     * 
     * @return false si no change detected
     */
    public boolean detectClassesChange() {
        return false;
    }

    /**
     * Called at application start (and at each reloading) Time to start stateful things.
     */
    public void onApplicationStart() {
    }

    /**
     * Called after the application start.
     */
    public void afterApplicationStart() {
    }

    /**
     * Called at application stop (and before each reloading) Time to shutdown stateful things.
     */
    public void onApplicationStop() {
    }

    /**
     * Called before a Play! invocation. Time to prepare request specific things.
     */
    public void beforeInvocation() {
    }

    /**
     * Called after an invocation. (unless an exception has been thrown). Time to close request specific things.
     */
    public void afterInvocation() {
    }

    /**
     * Called if an exception occurred during the invocation.
     * 
     * @param e
     *            The caught exception.
     */
    public void onInvocationException(Throwable e) {
    }

    /**
     * Called at the end of the invocation. (even if an exception occurred). Time to close request specific things.
     */
    public void invocationFinally() {
    }

    /**
     * Called before an 'action' invocation, ie an HTTP request processing.
     * 
     * @param actionMethod
     *            name of the method
     */
    public void beforeActionInvocation(Method actionMethod) {
    }

    /**
     * Called when the action method has thrown a result.
     * 
     * @param result
     *            The result object for the request.
     */
    public void onActionInvocationResult(Result result) {
    }

    public void onInvocationSuccess() {
    }

    /**
     * Called when the request has been routed.
     * 
     * @param route
     *            The route selected.
     */
    public void onRequestRouting(Route route) {
    }

    /**
     * Called at the end of the action invocation.
     */
    public void afterActionInvocation() {
    }

    /**
     * Called at the end of the action invocation (either in case of success or any failure).
     */
    public void onActionInvocationFinally() {
    }

    /**
     * Called when the application.conf has been read.
     */
    public void onConfigurationRead() {
    }

    /**
     * Called after routes loading.
     */
    public void onRoutesLoaded() {
    }

    /**
     * Event may be sent by plugins or other components
     * 
     * @param message
     *            convention: pluginClassShortName.message
     * @param context
     *            depends on the plugin
     */
    public void onEvent(String message, Object context) {
    }

    /**
     * @param modified
     *            list of modified class
     * @return List of class
     */
    public List<ApplicationClass> onClassesChange(List<ApplicationClass> modified) {
        return emptyList();
    }

    /**
     * @return List of the template extension
     */
    public List<String> addTemplateExtensions() {
        return emptyList();
    }

    /**
     * Override to provide additional mime types from your plugin. These mimetypes get priority over the default
     * framework mimetypes but not over the application's configuration.
     * 
     * @return a Map from extensions (without dot) to mimetypes
     */
    public Map<String, String> addMimeTypes() {
        return emptyMap();
    }

    /**
     * Let a chance to the plugin to compile it owns classes. Must be added to the mutable list.
     * 
     * @param classes
     *            list of class to compile
     * @deprecated
     */
    @Deprecated
    public void compileAll(List<ApplicationClass> classes) {
    }

    /**
     * Let some plugins route themself
     * 
     * @param request
     *            the current request
     */
    public void routeRequest(Request request) {
    }

    /**
     * @param modelClass
     *            class of the model
     * @return the Model factory
     */
    public Model.Factory modelFactory(Class<? extends Model> modelClass) {
        return null;
    }

    public void afterFixtureLoad() {
    }

    /**
     * Inter-plugin communication.
     * 
     * @param message
     *            the message to post
     * @param context
     *            an object
     */
    public static void postEvent(String message, Object context) {
        Play.pluginCollection.onEvent(message, context);
    }

    public void onApplicationReady() {
    }

    @Override
    public int compareTo(PlayPlugin o) {
        int res = index < o.index ? -1 : (index == o.index ? 0 : 1);
        if (res != 0) {
            return res;
        }

        // index is equal in both plugins.
        // Sort on class type to get consistent order
        res = this.getClass().getName().compareTo(o.getClass().getName());
        if (res != 0) {
            // classnames where different
            return res;
        }

        // Identical classnames.
        // Sort on instance to get consistent order.
        // We only return 0 (equal) if both identityHashCode are identical
        // which is only the case if both this and other are the same object instance.
        // This is consistent with equals() when no special equals-method is implemented.
        int thisHashCode = System.identityHashCode(this);
        int otherHashCode = System.identityHashCode(o);
        return (thisHashCode < otherHashCode ? -1 : (thisHashCode == otherHashCode ? 0 : 1));
    }

    public String overrideTemplateSource(BaseTemplate template, String source) {
        return null;
    }

    public Object willBeValidated(Object value) {
        return null;
    }

    /**
     * Implement to add some classes that should be considered unit tests but do not extend {@link org.junit.Assert} to
     * tests that can be executed by test runner (will be visible in test UI).
     * <p>
     * <strong>Note:</strong>You probably will also need to override {@link PlayPlugin#runTest(java.lang.Class)} method
     * to handle unsupported tests execution properly.
     * <p>
     * Keep in mind that this method can only add tests to currently loaded ones. You cannot disable tests this way. You
     * should also make sure you do not duplicate already loaded tests.
     * 
     * @return list of plugin supported unit test classes (empty list in default implementation)
     */
    public Collection<Class> getUnitTests() {
        return emptyList();
    }

    /**
     * Implement to add some classes that should be considered functional tests but do not extend
     * {@link play.test.FunctionalTest} to tests that can be executed by test runner (will be visible in test UI).
     * <p>
     * <strong>Note:</strong>You probably will also need to override {@link PlayPlugin#runTest(java.lang.Class)} method
     * to handle unsupported tests execution properly.
     * <p>
     * Keep in mind that this method can only add tests to currently loaded ones. You cannot disable tests this way. You
     * should also make sure you do not duplicate already loaded tests.
     *
     * @return list of plugin supported functional test classes (empty list in default implementation)
     */
    public Collection<Class> getFunctionalTests() {
        return emptyList();
    }

    /**
     * Class that define a filter. A filter is a class that wrap a certain behavior around an action. You can access
     * your Request and Response object within the filter. See the JPA plugin for an example. The JPA plugin wraps a
     * transaction around an action. The filter applies a transaction to the current Action.
     */
    public abstract static class Filter<T> {
        String name;

        public Filter(String name) {
            this.name = name;
        }

        public abstract T withinFilter(play.libs.F.Function0<T> fct) throws Throwable;

        /**
         * Surround innerFilter with this. (innerFilter after this)
         * 
         * @param innerFilter
         *            filter to be wrapped.
         * @return a new Filter object. newFilter.withinFilter(x) is
         *         outerFilter.withinFilter(innerFilter.withinFilter(x))
         */
        public Filter<T> decorate(final Filter<T> innerFilter) {
            final Filter<T> outerFilter = this;
            return new Filter<T>(this.name) {
                @Override
                public T withinFilter(F.Function0<T> fct) throws Throwable {
                    return compose(outerFilter.asFunction(), innerFilter.asFunction()).apply(fct);
                }
            };
        }

        /**
         * Compose two second order functions whose input is a zero param function that returns type T...
         * 
         * @param outer
         *            Function that will wrap inner -- ("outer after inner")
         * @param inner
         *            Function to be wrapped by outer function -- ("outer after inner")
         * @return A function that computes outer(inner(x)) on application.
         */
        private static <T> Function1<F.Function0<T>, T> compose(final Function1<F.Function0<T>, T> outer,
                final Function1<F.Function0<T>, T> inner) {

            return new Function1<F.Function0<T>, T>() {
                @Override
                public T apply(final F.Function0<T> arg) throws Throwable {
                    return outer.apply(new F.Function0<T>() {
                        @Override
                        public T apply() throws Throwable {
                            return inner.apply(arg);
                        }
                    });
                }
            };
        }

        private final Function1<play.libs.F.Function0<T>, T> _asFunction = new Function1<F.Function0<T>, T>() {
            @Override
            public T apply(F.Function0<T> arg) throws Throwable {
                return withinFilter(arg);
            }
        };

        public Function1<play.libs.F.Function0<T>, T> asFunction() {
            return _asFunction;
        }

        public String getName() {
            return name;
        }

        // I don't want to add any additional dependencies to the project or use JDK 8 features
        // so I'm just rolling my own 1 arg function interface... there must be a better way to do this...
        public static interface Function1<I, O> {
            public O apply(I arg) throws Throwable;
        }
    }

    public final boolean hasFilter() {
        return this.getFilter() != null;
    }

    /**
     * Return the filter implementation for this plugin.
     * 
     * @return filter object of this plugin
     */
    public Filter getFilter() {
        return null;
    }

}
