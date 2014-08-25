package cn.bran.play;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
// available only with newer commons-lang
// import org.apache.commons.lang.text.StrSubstitutor;

import play.Play;
//import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.exceptions.ActionNotFoundException;
import play.exceptions.NoRouteFoundException;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.mvc.ActionInvoker;
import play.mvc.Router;
import play.mvc.Http.Request;
import play.mvc.Router.ActionDefinition;
import play.mvc.Router.Route;
import play.templates.GroovyTemplate;
import cn.bran.japid.util.StringUtils;
import cn.bran.japid.util.UrlMapper;

/**
 * 
 * the logic is modeled after the the Play!'s {@code Template.ActionBridge.}
 * 
 * 
 * @author bran
 * 
 */
public class RouteAdapter implements UrlMapper {
	// String controllerName;

	/**
	 * 
	 * @param controllerName
	 *            the current controller's name. If null is specified, the
	 *            Request.current().controller will be used
	 */
	public RouteAdapter() {
		super();
		// this.controllerName = controllerName;
	}

	/**
	 * 
	 * @param actionString
	 *            the leasing part of the whole expression, e.g.
	 *            Application.show in @{Application.show(post.previous().id)}.
	 *            it can also bear the form of a simple function call, such as:
	 *            show
	 * @param params
	 *            the runtime value of the parameters used to call the action,
	 *            e.g. 123 from evaluating "post.previous().id"
	 */
	@Override
	public String lookup(String actionString, Object[] params) {
		ActionDefinition ad = lookupActionDefinition(actionString, params);
		return ad.toString();
	}

	public ActionDefinition lookupActionDefinition(String actionString,
			Object[] params) {
		ActionDefinition ad = new ActionBridge(false).invokeMethod(actionString, params);
		return ad;
	}

	@Override
	public String lookupAbs(String action, Object[] args) {
		return getBaseUrl() + this.lookup(action, args);
	}

	  // Gets baseUrl from current request or application.baseUrl in application.conf
	// copied from Play code
    protected static String getBaseUrl() {
        if (Request.current() == null) {
            // No current request is present - must get baseUrl from config
            String appBaseUrl = Play.configuration.getProperty("application.baseUrl", "application.baseUrl");
            if (appBaseUrl.endsWith("/")) {
                // remove the trailing slash
                appBaseUrl = appBaseUrl.substring(0, appBaseUrl.length()-1);
            }
            return appBaseUrl;
        } else {
            return Request.current().getBase();
        }
    }
    
	public String lookupStatic(String resource, boolean isAbs) {
		return reverseStaticLookup(resource, isAbs);
		// return Router.reverseWithCheck(resource,
		// Play.getVirtualFile(resource));
	}

	@Override
	public String lookupStaticAbs(String resource) {
//		return Request.current().getBase() + this.lookupStatic(resource);
		return this.lookupStatic(resource, true);
	}

	/**
	 * @return
	 */
	private static HashMap<String, String> getActionCache() {
		HashMap<String, String> hash = (HashMap<String, String>) Request.current().args.get("actionReverseCache");
		if (hash == null) {
			hash = new HashMap<String, String>();
			Request.current().args.put("actionReverseCache", hash);
		}

		// put in the threadlocal is a problem because threads are reused in the
		// pool
		// HashMap<String, String> hash = actionReverseCache.get();
		// if (hash == null) {
		// hash = new HashMap<String, String>();
		// actionReverseCache.set(hash);
		// }
		return hash;
	}

	/**
	 * @return
	 */
	private static HashMap<String, String[]> getActionParamCache() {
		// the cache is bound to a request
		HashMap<String, String[]> hash = (HashMap<String, String[]>) Request.current().args.get("actionParamNamesCache");
		if (hash == null) {
			hash = new HashMap<String, String[]>();
			Request.current().args.put("actionParamNamesCache", hash);
		}

		// HashMap<String, String[]> hash = actionParamNamesCache.get();
		// if (hash == null) {
		// hash = new HashMap<String, String[]>();
		// actionParamNamesCache.set(hash);
		// }
		return hash;
	}

	/**
	 * reverse lookup a static resource
	 * 
	 * provide cache
	 * 
	 * @author Bing Ran<bing_ran@hotmail.com>
	 * @param resource
	 * @return
	 */
	public static String reverseStaticLookup(String resource, boolean isAbs) {
		try {
			HashMap<String, String> hash = getStaticCache();
			String url = hash.get(resource);
			if (url == null) {
				url = Router.reverseWithCheck(resource, Play.getVirtualFile(resource), isAbs);
				hash.put(resource, url);
			}
			return url;
		} catch (RuntimeException e) {
			throw new RuntimeException(e + ". No matching route in reverse lookup: " + resource);
		}
	}

	/**
	 * @return
	 */
	private static HashMap<String, String> getStaticCache() {
		HashMap<String, String> hash = staticCache.get();
		if (hash == null) {
			hash = new HashMap<String, String>();
			staticCache.set(hash);
		}
		return hash;
	}

	// cache lookups on the current thread.
	// ideally they can be done in the router for longer persistence and be
	// synched with route table reloading.

	// store quick reverse lookup
	// TODO should consider concurrent hashmap, mm but it's only thread local!
	// should this be shared among all thread(in the case concurrentThreadLocal
	// is required
	//
	static ThreadLocal<HashMap<String, String>> staticCache = new ThreadLocal<HashMap<String, String>>();
	// <action & param hash, url pattern>
	static ThreadLocal<HashMap<String, String>> actionReverseCache = new ThreadLocal<HashMap<String, String>>();
	// <action, paramNames>
	static ThreadLocal<HashMap<String, String[]>> actionParamNamesCache = new ThreadLocal<HashMap<String, String[]>>();

	@Override
	public String lookupStatic(String resource) {
		return lookupStatic(resource, false);
	}

}
