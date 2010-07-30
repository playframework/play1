package play;

import java.lang.annotation.Annotation;
import com.google.gson.JsonObject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.db.Model;
import play.db.ModelLoader;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router.Route;
import play.mvc.results.Result;
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

    public TestResults runTest(Class<BaseTest> clazz) {
        return null;
    }

    /**
     * Called when play need to bind a Java object from HTTP params
     */
    public Object bind(String name, Class clazz, java.lang.reflect.Type type, Annotation[] annotations, Map<String, String[]> params) {
        return null;
    }

    /**
     * Retun the plugin status
     */
    public String getStatus() {
        return null;
    }

    /**
     * Retun the plugin status in JSON format
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
     * Let the plugin to modify the parsed template.
     * @param template
     */
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
     * Let a chance to this plugin to manage a static ressource
     * @param request The Play request
     * @param response The Play response
     * @return true if this plugin has managed this request
     */
    public boolean serveStatic(VirtualFile file, Request request, Response response) {
        return false;
    }

    public void beforeDetectingChanges() {
    }

    /**
     * It's time for the plugin to detect changes.
     * Throw an exception is the application must be reloaded.
     */
    public void detectChange() {
    }

    /**
     * Called at application start (and at each reloading)
     * Time to start statefull things.
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
     * Time to shutdown statefull things.
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
     * Called when the application.cond has been read.
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

    public void onClassesChange(List<ApplicationClass> modified) {
    }

    public List<String> addTemplateExtensions() {
        return new ArrayList<String>();
    }

    /**
     * Let a chance to the plugin to compile it owns classes.
     * Must be added to the mutable list.
     */
    public void compileAll(List<ApplicationClass> classes) {
    }

    /**
     * Let some plugins route themself
     * @param request
     */
    public void routeRequest(Request request) {
    }

    public ModelLoader modelLoader(Class<Model> modelClass) {
        return null;
    }

    public void afterFixtureLoad() {
    }

    /**
     * Inter-plugin communication.
     */
    public static void postEvent(String message, Object context) {
        List<PlayPlugin> plugins = Play.plugins;
        for (PlayPlugin playPlugin : plugins) {
            playPlugin.onEvent(message, context);
        }
    }

    // ~~~~~
    public int compareTo(PlayPlugin o) {
        return (index < o.index ? -1 : (index == o.index ? 0 : 1));
    }
}
