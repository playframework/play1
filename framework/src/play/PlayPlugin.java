package play;

import java.lang.reflect.Method;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.Result;
import play.vfs.VirtualFile;

/**
 * A framework plugin
 */
public abstract class PlayPlugin implements Comparable<PlayPlugin> {

    /**
     * Priority
     */
    public int index;

    /**
     * Called at plugin loading
     */
    public void onLoad() {
    }
    
    public void enhance(ApplicationClass applicationClass) {
    }
    
    /**
     * Let a chance to this plugin to fully manage this request
     * @param request The Play request
     * @param response The Play response
     * @return true if this plugin has managed this request
     */
    public boolean rawInvocation(Request request, Response response) throws Exception{
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
     * Let some plugins route themself
     * @param request
     */
    public void routeRequest(Request request) {
    }

    public int compareTo(PlayPlugin o) {
        return (index < o.index ? -1 : (index == o.index ? 0 : 1));
    }
}
