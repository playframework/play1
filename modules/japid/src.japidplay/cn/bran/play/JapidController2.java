package cn.bran.play;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import play.Play;
import play.Play.Mode;
import play.cache.Cache;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.mvc.After;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Finally;
import play.mvc.Http.Request;
import play.mvc.results.RenderTemplate;
import cn.bran.japid.compiler.NamedArg;
import cn.bran.japid.compiler.NamedArgRuntime;
import cn.bran.japid.rendererloader.RendererClass;
import cn.bran.japid.template.ActionRunner;
import cn.bran.japid.template.JapidTemplateBaseWithoutPlay;
import cn.bran.japid.template.RenderResult;
import cn.bran.japid.util.DirUtil;
import cn.bran.japid.util.RenderInvokerUtils;
import cn.bran.japid.util.StackTraceUtils;

/**
 * a helper class. for hiding the template API from user eyes. not really needed
 * since the template invocation API is simple enough.
 * 
 * In comparison to JapidController, JapidController2 utilizes JapidRenderer to
 * load template classes, which is independent of Play's class loading
 * mechanism.
 * 
 * Decoupling of template class loading from Play's class loading potentially
 * results in clean and faster class loading and reloading when Play refreshes
 * the classes.
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * @deprecated responsibility has been integrated in JapidController. 
 */
@Deprecated
public class JapidController2 extends Controller {
	public static final char DOT = '.';
	public static final String HTML = ".html";

	/**
	 * render an array of objects to a template defined by a Template class.
	 * 
	 * @param <T>
	 *            a sub-class type of JapidTemplateBase
	 * @param c
	 *            a sub-class of JapidTemplateBase
	 * @param args
	 *            arguments
	 */
	public static <T extends JapidTemplateBaseWithoutPlay> void render(Class<T> c, Object... args) {
		try {
			RenderResult rr = invokeRender(c, args);
			throw new JapidResult(rr);
		} catch (Exception e) {
			if (e instanceof JapidResult)
				throw (JapidResult) e;
			// e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param <T>
	 * @param c
	 * @param args
	 * @return
	 * @throws NoSuchMethodException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private static <T extends JapidTemplateBaseWithoutPlay> RenderResult invokeRender(Class<T> c, Object... args) {
		try {
			return RenderInvokerUtils.invokeRender(c, args);
		} catch (Throwable e) {
			JapidPlayRenderer.handleException(e);
			return null;
		}
	}

	protected static <T extends JapidTemplateBaseWithoutPlay> RenderResult invokeNamedArgsRender(Class<T> c,
			NamedArgRuntime[] args) {
		try {
			return RenderInvokerUtils.invokeNamedArgsRender(c, args);
		} catch (Throwable e) {
			JapidPlayRenderer.handleException(e);
			return null;
		}
	}

	/**
	 * just hide the result throwing
	 * 
	 * @param rr
	 */
	protected static void render(RenderResult rr) {
		throw new JapidResult(rr);
	}

	/**
	 * pickup the Japid renderer in the conventional location and render it.
	 * Positional match is used to assign values to parameters
	 * 
	 * TODO: the signature would be confusing for cases where there is a single
	 * argument and the type is an array! In that case the user must cast it to
	 * Object: <code>renderJapid((Object)myArray);</code>
	 * 
	 * @param objects
	 */
	protected static void renderJapid(Object... objects) {
		String action = template();
		renderJapidWith(action, objects);
	}

	protected static void renderJapidByName(NamedArgRuntime... namedArgs) {
		String action = template();
		renderJapidWith(action, namedArgs);
	}

	protected static void renderJapidEager(Object... objects) {
		String action = template();
		renderJapidWithEager(action, objects);
	}

	public static void renderJapidWith(String template, Object... args) {
		throw new JapidResult(getRenderResultWith(template, args));
	}

	public static void renderJapidWith(String template, NamedArgRuntime[] namedArgs) {
		throw new JapidResult(getRenderResultWith(template, namedArgs));
	}

	public static void renderJapidWithEager(String template, Object... args) {
		throw new JapidResult(getRenderResultWith(template, args)).eval();
	}

	public static String template() {
		// the super.template() class uses current request object to determine
		// the caller and method to find the matching template
		// this won't work if the current method is called from another action.
		// let's fall back to use the stack trace to deduce the template.
		// String caller2 = StackTraceUtils.getCaller2();

		final StackTraceElement[] stes = new Throwable().getStackTrace();
		// let's iterate back in the stacktrace to find the recent action calls.
		for (StackTraceElement st : stes) {
			String controller = st.getClassName();
			String action = st.getMethodName();
			ApplicationClass conAppClass = Play.classes.getApplicationClass(controller);
			if (conAppClass != null) {
				Class controllerClass = conAppClass.javaClass;
				if (JapidController2.class.isAssignableFrom(controllerClass)) {
					Method actionMethod = /* Java. */findActionMethod(action, controllerClass);
					if (actionMethod != null) {
						String expr = controller + "." + action;
						// content negotiation
						String format = Request.current().format;
						if ("html".equals(format)) {
							return expr;
						} else {
							String expr_format = expr + "_" + format;
							if (expr_format.startsWith("controllers.")) {
								expr_format = "japidviews" + expr_format.substring(expr_format.indexOf('.'));
							}
							RendererClass rc = JapidPlayRenderer.japidClasses.get(expr_format);
							if (rc != null)
								return expr_format;
							else {
								// fall back
								return expr;
							}
						}
					}
				}
			}
		}
		throw new RuntimeException(
				"The calling stack does not contain a valid controller. Should not have happended...");
	}

	/**
	 * copies from the same method in the Java class. Removed the public
	 * requirement for easier chaining.
	 * 
	 * @param name
	 * @param clazz
	 * @return
	 */
	public static Method findActionMethod(String name, Class clazz) {
		while (!clazz.getName().equals("java.lang.Object")) {
			for (Method m : clazz.getDeclaredMethods()) {
				if (m.getName().equalsIgnoreCase(name) /*
														 * &&
														 * Modifier.isPublic(m
														 * .getModifiers())
														 */) {
					// Check that it is not an intercepter
					if (!m.isAnnotationPresent(Before.class) && !m.isAnnotationPresent(After.class)
							&& !m.isAnnotationPresent(Finally.class)) {
						return m;
					}
				}
			}
			clazz = clazz.getSuperclass();
		}
		return null;
	}

	public static RenderResult getRenderResultWith(String template, NamedArgRuntime[] args) {
		try {
			String templateClassName = getTemapletClassName(template);
			Class<? extends JapidTemplateBaseWithoutPlay> tClass = getRenderClass(templateClassName);
			return RenderInvokerUtils.invokeNamedArgsRender(tClass, args);
		} catch (Throwable e) {
			return JapidPlayRenderer.handleException(e);
		}

	}

	private static Class<? extends JapidTemplateBaseWithoutPlay> getRenderClass(String templateClassName) {
		Class<? extends JapidTemplateBaseWithoutPlay> tClass = JapidPlayRenderer.getTemplateClass(templateClassName);

		if (tClass == null) {
			String templateFileName = templateClassName.replace(DOT, '/') + HTML;
			throw new RuntimeException("Could not find a Japid template with the name of: " + templateFileName);
		}
		return tClass;
	}

	/**
	 * render parameters to the prescribed template and return the RenderResult
	 * 
	 * @param template
	 *            relative path from japidviews folder. if empty, use implicit
	 *            naming pattern to match the template
	 * @param args
	 */
	public static RenderResult getRenderResultWith(String template, Object... args) {
		try {
			String templateClassName = JapidController.getTemapletClassName(template);
			Class<? extends JapidTemplateBaseWithoutPlay> tClass = getRenderClass(templateClassName);
			return invokeRender(tClass, args);
		} catch (Throwable e) {
			return JapidPlayRenderer.handleException(e);
		}
	}

	// protected static String caller() {
	// String action = StackTraceUtils.getCaller();
	// return action;
	// }
	/**
	 * cache a Japid RenderResult associated with an action call with specific
	 * arguments
	 * 
	 * To use this method and the getFromCache() effectively requires the each
	 * of the argument has a toString() that uniquely identify itself. Otherwise
	 * the user needs to provider its own key building routine
	 * 
	 * mind the cost associated with this
	 * 
	 * @param rr
	 *            the render result to cache
	 * @param ttl
	 *            the expiration spec as in Play's Cache.set(), e.g., 1s, 2mn,
	 *            3h, etc
	 * @param objs
	 *            the original arguments
	 */
	protected static void cache(RenderResult rr, String ttl, Object... objs) {
		String caller = buildKey(null, objs);
		Cache.set(caller, rr, ttl);
	}

	/**
	 * use a defined keybase to build the key and cache the RenderResult by that
	 * key
	 * 
	 * @param rr
	 * @param ttl
	 * @param keyBase
	 * @param objs
	 */
	protected static void cache(RenderResult rr, String ttl, String keyBase, Object... objs) {
		String caller = buildKey(keyBase, objs);
		Cache.set(caller, rr, ttl);
	}

	/**
	 * mind the cost associated with this and the key building issues, as stated
	 * in the cache() method
	 * 
	 * @param objs
	 * @return
	 */

	protected static RenderResult getFromCache(Object... objs) {
		// the key building with caller info and the arguments
		if (RenderResultCache.shouldIgnoreCache())
			return null;
		String caller = buildKey(null, objs);
		Object object = Cache.get(caller);
		if (object instanceof RenderResult) {
			return (RenderResult) object;
		} else {
			return null;
		}
	}

	/**
	 * 
	 * @param keyBase
	 *            usually the fully qualified method name of the controller
	 *            action
	 * @param objs
	 * @return
	 */
	protected static RenderResult getFromCache(String keyBase, Object... objs) {
		// the key building with caller info and the arguments
		if (RenderResultCache.shouldIgnoreCache())
			return null;
		String caller = buildKey(keyBase, objs);
		Object object = Cache.get(caller);
		if (object instanceof RenderResult) {
			return (RenderResult) object;
		} else {
			return null;
		}
	}

	/**
	 * @param objs
	 * @return
	 */
	private static String buildKey(String base, Object... objs) {
		// the getCaller thing is relatively expensive, as it might take
		// hundreds of us to complete.

		String caller = base;
		if (base == null)
			caller = StackTraceUtils.getCaller2(); // tricky and expensive
		for (Object o : objs) {
			caller += "-" + String.valueOf(o);
		}
		return caller;
	}

	/**
	 * run a piece of rendering code with cache check and refilling
	 * 
	 * @param runner
	 * @param ttl
	 * @param objects
	 * 
	 * @deprecated use CacheableRunner directly in actions
	 */
	protected static void runWithCache(ActionRunner runner, String ttl, Object... objects) {
		if (ttl == null || ttl.trim().length() == 0)
			throw new RuntimeException("Cache expiration time must be defined.");

		ttl = ttl.trim();
		if (Character.isDigit(ttl.charAt(ttl.length() - 1))) {
			// assuming second
			ttl += "s";
		}

		String base = StackTraceUtils.getCaller();
		RenderResult rr = getFromCache(base, objects);
		if (rr == null) {
			rr = runner.run();
			cache(rr, ttl, base, objects);
		}
		// System.out.println("render show took ms: " + rr.getRenderTime());
		throw new JapidResult(rr);
	}

	/**
	 * 
	 * @param runner
	 * @param ttl
	 * @deprecated use CacheableRunner directly in actions
	 */
	protected static void runWithCache(ActionRunner runner, String ttl) {
		runWithCache(runner, ttl, new Object[] {});
	}

	/**
	 * run action wrapped in a CacheableRunner and throws a JapidResult to the
	 * downstream of the pipeline
	 * 
	 * @param r
	 */
	protected static void render(CacheableRunner r) {
		RenderResult rr = r.run();
		throw new JapidResult(rr);
	}

	/**
	 * set a flag to instruct the cache runner to bypass cache checking for
	 * reading but still cache the result
	 */
	public static void ignoreCache() {
		RenderResultCache.setIgnoreCache(true);
	}

	/**
	 * set a flag to instruct the cache runner to bypass cache checking for
	 * reading but still cache the result, for the current response and next
	 * request
	 */
	public static void ignoreCacheNowAndNext() {
		RenderResultCache.setIgnoreCacheInCurrentAndNextReq(true);
	}

	/**
	 * Evict a cached Japid result resulted from a Japid directive
	 * <em>invoke<em/>. 
	 * Can be used in a controller action to invalidate a cached result after a relevant state has been changed.
	 * 
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @param controllerClass
	 *            the controller class
	 * @param actionName
	 *            action name
	 * @param args
	 *            the arguments to the action method.
	 */
	public static <C extends JapidController2> void evictJapidResultCache(Class<C> controllerClass, String actionName,
			Object... args) {
		CacheablePlayActionRunner.deleteCache(controllerClass, actionName, args);
	}

	public static void evictJapidResultCache(String key) {
		CacheablePlayActionRunner.deleteCache(key);
	}

	/**
	 * this will set a flag so calling another action won't trigger a redirect
	 */
	protected static void dontRedirect() {
		play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation.initActionCall();
	}

	/**
	 * render a text in a RenderResult so it can work with invoke tag in
	 * templates.
	 * 
	 * @param s
	 */
	protected static void renderText(String s) {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "text/plain; charset=utf-8");
		render(new RenderResult(headers, new StringBuilder(s), -1L));
	}

	protected static void renderText(Object o) {
		String str = o == null ? "" : o.toString();
		renderText(str);
	}

	protected static void renderText(int o) {
		renderText(new Integer(o));
	}

	protected static void renderText(long o) {
		renderText(new Long(o));
	}

	protected static void renderText(float o) {
		renderText(new Float(o));
	}

	protected static void renderText(double o) {
		renderText(new Double(o));
	}

	protected static void renderText(boolean o) {
		renderText(new Boolean(o));
	}

	protected static void renderText(char o) {
		renderText(new String(new char[] { o }));
	}

	/**
	 * run another action wrapped in a runnable run() and intercept the Result
	 * 
	 * one should wrap the call to another action like this: new Runnable () {
	 * public void run() { AnotherController.action();} }
	 * 
	 * @param runnable
	 */
	protected static String getResultFromAction(Runnable runnable) {
		dontRedirect();
		try {
			runnable.run();
			System.out
					.println("JapidController.getResultFromAction() warning: the runnable did not generate a result.");
			return "";
		} catch (JapidResult e) {
			return e.extractContent();
			// TODO: handle exception
		} catch (RenderTemplate rt) {
			return rt.getContent();
		} catch (Exception ex) {
			ex.printStackTrace();
			return "";
		}
	}

	/**
	 * run another action wrapped in a runnable run() and intercept the Result,
	 * ignoring the cache
	 * 
	 * one should wrap the call to another action like this: new Runnable () {
	 * public void run() { AnotherController.action();} }
	 * 
	 * @param runnable
	 */
	protected static String getFreshResultFromAction(Runnable runnable) {
		ignoreCache();
		return getResultFromAction(runnable);
	}

	/**
	 * can be used to generate a key based on the query
	 * 
	 * @return
	 */
	public static String genCacheKey() {
		return "japidcache:" + Request.current().action + ":" + Request.current().querystring;
	}

	protected static NamedArgRuntime named(String name, Object val) {
		return new NamedArgRuntime(name, val);
	}

	static String runnerName = CacheablePlayActionRunner.class.getName();

	/**
	 * determine if the current stack frame is a descendant of
	 * CacheablePlayActionRunner which is used when invoking actions from Japid
	 * views
	 * 
	 * @return
	 */
	public static boolean isInvokedfromJapidView() {
		Throwable t = new Throwable();
		// t.printStackTrace();
		final StackTraceElement[] ste = t.getStackTrace();
		for (int i = 0; i < ste.length; i++) {
			StackTraceElement st = ste[i];
			String className = st.getClassName();
			if (className.equals(runnerName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param template
	 * @return
	 */
	public static String getTemapletClassName(String template) {
		//
		if (template == null || template.length() == 0) {
			template = template();
		}

		if (template.endsWith(HTML)) {
			template = template.substring(0, template.length() - HTML.length());
		}

		// String action = StackTraceUtils.getCaller(); // too tricky to use
		// stacktrace to track the caller action name
		// something like controllers.japid.SampleController.testFindAction

		if (template.startsWith("@")) {
			// a template in the current directory
			template = Request.current().controller + "/" + template.substring(1);
		}

		// map to default japid view
		if (template.startsWith("controllers.")) {
			template = template.substring(template.indexOf(DOT) + 1);
		}
		String templateClassName = template.startsWith(DirUtil.JAPIDVIEWS_ROOT) ? template : DirUtil.JAPIDVIEWS_ROOT
				+ File.separator + template;

		templateClassName = templateClassName.replace('/', DOT).replace('\\', DOT);
		return templateClassName;
	}
}
