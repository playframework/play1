package play;

import java.lang.annotation.Annotation;
import com.google.gson.JsonObject;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.data.binding.RootParamNode;
import play.db.Model;
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
     */
    public TestResults runTest(Class<BaseTest> clazz) {
        return null;
    }

    /**
     * Use method using RootParamNode instead
     * @return
     */
    @Deprecated
    public Object bind(String name, Class clazz, Type type, Annotation[] annotations, Map<String, String[]> params) {
        return null;
    }

    /**
     * Called when play need to bind a Java object from HTTP params.
     *
     * When overriding this method, do not call super impl.. super impl is calling old bind method
     * to be backward compatible.
     */
    public Object bind( RootParamNode rootParamNode, String name, Class<?> clazz, Type type, Annotation[] annotations) {
        // call old method to be backward compatible
        return bind(name, clazz, type, annotations, rootParamNode.originalParams);
    }

    /**
     * Use bindBean instead
     */
    @Deprecated
    public Object bind(String name, Object o, Map<String, String[]> params) {
        return null;
    }

    /**
     * Called when play need to bind an existing Java object from HTTP params.
     * When overriding this method, DO NOT call the super method, since its default impl is to
     * call the old bind method to be backward compatible.
     */
    public Object bindBean(RootParamNode rootParamNode, String name, Object bean) {
        // call old method to be backward compatible.
        return bind(name, bean, rootParamNode.originalParams);
    }

    public Map<String, Object> unBind(Object src, String name) {
        return null;
    }
    
    /**
     * Translate the given key for the given locale and arguments.
     * If null is returned, Play's normal message translation mechanism will be
     * used.
     */
    public String getMessage(String locale, Object key, Object... args) {
        return null;
    }

    /**
     * Return the plugin status
     */
    public String getStatus() {
        return null;
    }

    /**
     * Return the plugin status in JSON format
     */
    public JsonObject getJsonStatus() {
        return null;
    }

    /**
     * Enhance this class
     * @param applicationClass
     * @throws java.lang.Exception
     */
    public void enhance(ApplicationClass applicationClass) throws Exception {
    }

    /**
     * This hook is not plugged, don't implement it
     * @param template
     */
    @Deprecated
    public void onTemplateCompilation(Template template) {
    }

    /**
     * Give a chance to this plugin to fully manage this request
     * @param request The Play request
     * @param response The Play response
     * @return true if this plugin has managed this request
     */
    public boolean rawInvocation(Request request, Response response) throws Exception {
        return false;
    }

    /**
     * Let a chance to this plugin to manage a static resource
     * @param request The Play request
     * @param response The Play response
     * @return true if this plugin has managed this request
     */
    public boolean serveStatic(VirtualFile file, Request request, Response response) {
        return false;
    }

    public void beforeDetectingChanges() {
    }

    public Template loadTemplate(VirtualFile file) {
        return null;
    }

    /**
     * It's time for the plugin to detect changes.
     * Throw an exception is the application must be reloaded.
     */
    public void detectChange() {
    }

    /**
     * It's time for the plugin to detect changes.
     * Throw an exception is the application must be reloaded.
     */
    public boolean detectClassesChange() {
        return false;
    }

    /**
     * Called at application start (and at each reloading)
     * Time to start stateful things.
     */
    public void onApplicationStart() {
    }

    /**
     * Called after the application start.
     */
    public void afterApplicationStart() {
    }

    /**
     * Called at application stop (and before each reloading)
     * Time to shutdown stateful things.
     */
    public void onApplicationStop() {
    }

    /**
     * Called before a Play! invocation.
     * Time to prepare request specific things.
     */
    public void beforeInvocation() {
    }

    /**
     * Called after an invocation.
     * (unless an excetion has been thrown).
     * Time to close request specific things.
     */
    public void afterInvocation() {
    }

    /**
     * Called if an exception occured during the invocation.
     * @param e The catched exception.
     */
    public void onInvocationException(Throwable e) {
    }

    /**
     * Called at the end of the invocation.
     * (even if an exception occured).
     * Time to close request specific things.
     */
    public void invocationFinally() {
    }

    /**
     * Called before an 'action' invocation,
     * ie an HTTP request processing.
     */
    public void beforeActionInvocation(Method actionMethod) {
    }

    /**
     * Called when the action method has thrown a result.
     * @param result The result object for the request.
     */
    public void onActionInvocationResult(Result result) {
    }

    public void onInvocationSuccess() {
    }

    /**
     * Called when the request has been routed.
     * @param route The route selected.
     */
    public void onRequestRouting(Route route) {
    }

    /**
     * Called at the end of the action invocation.
     */
    public void afterActionInvocation() {
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
     * @param message convention: pluginClassShortName.message
     * @param context depends on the plugin
     */
    public void onEvent(String message, Object context) {
    }

    public List<ApplicationClass> onClassesChange(List<ApplicationClass> modified) {
        return new ArrayList<ApplicationClass>();
    }

    public List<String> addTemplateExtensions() {
        return new ArrayList<String>();
    }

    /**
     * Override to provide additional mime types from your plugin. These mimetypes get priority over
     * the default framework mimetypes but not over the application's configuration.
     * @return a Map from extensions (without dot) to mimetypes
     */
    public Map<String, String> addMimeTypes() {
        return new HashMap<String, String>();
    }

    /**
     * Let a chance to the plugin to compile it owns classes.
     * Must be added to the mutable list.
     */
    @Deprecated
    public void compileAll(List<ApplicationClass> classes) {
    }

    /**
     * Let some plugins route themself
     * @param request
     */
    public void routeRequest(Request request) {
    }

    public Model.Factory modelFactory(Class<? extends Model> modelClass) {
        return null;
    }

    public void afterFixtureLoad() {
    }

    /**
     * Inter-plugin communication.
     */
    public static void postEvent(String message, Object context) {
        Play.pluginCollection.onEvent(message, context);
    }

    public void onApplicationReady() {
    }

    // ~~~~~
    public int compareTo(PlayPlugin o) {
        int res = index < o.index ? -1 : (index == o.index ? 0 : 1);
        if (res!=0) {
            return res;
        }

        // index is equal in both plugins.
        // sort on classtype to get consistent order
        res = this.getClass().getName().compareTo(o.getClass().getName());
        if (res != 0 ) {
            // classnames where different
            return res;
        }

        // identical classnames.
        // sort on instance to get consistent order.
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
    
}
