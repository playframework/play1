package cn.bran.play;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import play.Logger;
import play.Play;
import play.Play.Mode;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.ApplicationClassloaderState;
import play.exceptions.CompilationException;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.mvc.Router.Route;
import play.mvc.Scope.Flash;
import play.mvc.results.Result;
import play.templates.Template;
import play.vfs.VirtualFile;
import cn.bran.japid.compiler.JapidCompilationException;
import cn.bran.japid.template.JapidRenderer;
import cn.bran.japid.template.JapidTemplateBaseWithoutPlay;
import cn.bran.japid.util.JapidFlags;
import cn.bran.japid.util.StringUtils;
import cn.bran.play.routing.AutoPath;
import cn.bran.play.routing.RouterClass;
import cn.bran.play.routing.RouterMethod;

/**
 * 
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * 
 */
public class JapidPlugin extends PlayPlugin {
	/**
	 * 
	 */
	private static final String WHEN_REQ_IN = "_whenRaw";
	public static final String ACTION_METHOD = "__actionMethod";
	private static final String RENDER_JAPID_WITH = "/renderJapidWith";
	private static final String NO_CACHE = "no-cache";
	private static AtomicLong lastTimeChecked = new AtomicLong(0);
	// can be used to cache a plugin scoped values
	private static Map<String, Object> japidCache = new ConcurrentHashMap<String, Object>();
	private String appPath;


	/**
	 * pre-compile the templates so PROD mode would work
	 */
	@Override
	public void onLoad() {
		Logger.info("JapidPlugin.onload().");
		if (Play.mode == Mode.DEV) {
			Logger.info("[Japid] version " + JapidRenderer.VERSION + " in DEV mode. Detecting japid scripts on classpath...");
			beforeDetectingChanges();
		} else {
			String isPrecompiling = System.getProperty("precompile");
			if (isPrecompiling != null && (isPrecompiling.equals("yes") || isPrecompiling.equals("true"))) {
				Logger.info("[Japid] version " + JapidRenderer.VERSION + " in PROD mode with precompiling: detect japid template changes.");
				beforeDetectingChanges();
			} else { // PROD mode and not pre-compile
				Logger.info("[Japid] version " + JapidRenderer.VERSION + " in PROD mode without pre-compiling. If the Japid templates have not been compiled to Java code, please run 'play japid:gen' before starting the application.");
			}
		}
		getDumpRequest();
		setupInjectTemplateBorder();
	}

	/**
	 * @author Bing Ran (bing.ran@hotmail.com)
	 */
	private void setupInjectTemplateBorder() {
		String property = Play.configuration.getProperty("japid.trace.file", "false");
		if ("on".equalsIgnoreCase(property) || "yes".equalsIgnoreCase(property))
			property = "true";
		JapidTemplateBaseWithoutPlay.globalTraceFile = new Boolean(property);

		property = Play.configuration.getProperty("japid.trace.file.html", "false");
		if ("on".equalsIgnoreCase(property) || "yes".equalsIgnoreCase(property))
			property = "true";
		JapidTemplateBaseWithoutPlay.globalTraceFileHtml = new Boolean(property);

		property = Play.configuration.getProperty("japid.trace.file.json", "false");
		if ("on".equalsIgnoreCase(property) || "yes".equalsIgnoreCase(property))
			property = "true";
		JapidTemplateBaseWithoutPlay.globalTraceFileJson = new Boolean(property);
	}

	@Override
	public void onConfigurationRead() {
		// appPath = Play.configuration.getProperty("context", "/");
	}

	public static Map<String, Object> getCache() {
		return japidCache;
	}

	/**
	 * 
	 */
	private void getDumpRequest() {
		String property = Play.configuration.getProperty("japid.dump.request");
		this.dumpRequest = property;
	}

	@Override
	public void beforeDetectingChanges() {
//		logDuration("beforeDetectingChanges");
		// have a delay in change detection.
		if (System.currentTimeMillis() - lastTimeChecked.get() < 1000)
			return;
		try {
			List<File> changed = JapidCommands.reloadChanged();
			if (changed.size() > 0) {
				for (File f : changed) {
					// System.out.println("pre-detect changed: " + f.getName());
				}
			}
		} catch (JapidCompilationException e) {
			// turn japid compilation error to Play's template error to get
			// better error reporting
			JapidPlayTemplate jpt = new JapidPlayTemplate();
			jpt.name = e.getTemplateName();
			jpt.source = e.getTemplateSrc();
			// throw new TemplateExecutionException(jpt, e.getLineNumber(),
			// e.getMessage(), e);
			VirtualFile vf = VirtualFile.fromRelativePath("/app/" + e.getTemplateName());
			throw new CompilationException(vf, "\"" + e.getMessage() + "\"", e.getLineNumber(), 0, 0);
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			throw e;
		}

		boolean hasRealOrphan = JapidCommands.rmOrphanJava();
		lastTimeChecked.set(System.currentTimeMillis());

		if (hasRealOrphan) {
			// a little messy here. clean the cache in case bad files are delete
			// remove all the existing ApplicationClass will reload everything.
			// ideally we just need to remove the orphan. But the internal cache
			// is not visible. Need API change to do that.
			Play.classes.clear();
			throw new RuntimeException("found orphan template Java artifacts. reload to be safe.");
		}
	}

	// // VirtualFile appRoot = VirtualFile.open(Play.applicationPath);
	// TranslateTemplateTask t = new TranslateTemplateTask();
	// {
	// Project proj = new Project();
	// t.setProject(proj);
	// proj.init();
	//
	// t.setSrcdir(new File("app"));
	// t.setIncludes(JAPIDVIEWS_ROOT + "/**/*.html");
	// t.importStatic(JapidPlayAdapter.class);
	// t.importStatic(Validation.class);
	// t.importStatic(JavaExtensions.class);
	// t.addAnnotation(NoEnhance.class);
	// t.addImport(JAPIDVIEWS_ROOT + "._layouts.*");
	// t.addImport(JAPIDVIEWS_ROOT + "._tags.*");
	// t.addImport("models.*");
	//
	// // t.add(new ModifiedSelector());
	// t.setTaskType("foo");
	// t.setTaskName("foo");
	// t.setOwningTarget(new Target());
	// }

	@Override
	public void onApplicationReady() {
		// appPath = Play.ctxPath; // won't work due to a bug in the servlet
		// wrapper. see onRoutesLoaded()
		if (Play.mode == Mode.PROD) {
			JapidPlayRenderer.init();
		}
	}

	@Override
	public void onApplicationStop() {
		try {

		} catch (Exception e) {
			throw new UnexpectedException(e);
		}
	}

	/**
	 * just before an action is invoked. See {@code ActionInvoker}
	 */
	@Override
	public void beforeActionInvocation(Method actionMethod) {
//		logDuration("beforeActionInvocation");
		
		final String actionName = actionMethod.getDeclaringClass().getName() + "." + actionMethod.getName();
		JapidController.threadData.get().put(ACTION_METHOD, actionName);

		String property = Play.configuration.getProperty("japid.dump.request");
		if (property != null && property.length() > 0) {
			if (!"false".equals(property) && !"no".equals(property)) {
				if ("yes".equals(property) || "true".equals(property)) {
					System.out.println("--- action ->: " + Request.current().action);
				} else if (Request.current().url.matches(property)) {
					System.out.println("--- action ->: " + Request.current().action);
				}
			}
		}

		String string = Flash.current().get(RenderResultCache.READ_THRU_FLASH);
		if (string != null) {
			RenderResultCache.setIgnoreCache(true);
		} else {
			// cache-control in lower case, lowercase for some reason
			Header header = Request.current().headers.get("cache-control");
			if (header != null) {
				List<String> list = header.values;
				if (list.contains(NO_CACHE)) {
					RenderResultCache.setIgnoreCache(true);
				}
			} else {
				header = Request.current().headers.get("pragma");
				if (header != null) {
					List<String> list = header.values;
					if (list.contains(NO_CACHE)) {
						RenderResultCache.setIgnoreCache(true);
					}
				} else {
					// just in case
					RenderResultCache.setIgnoreCacheInCurrentAndNextReq(false);
				}
			}
		}

		// // shortcut
		// String path = Request.current().path;
		// // System.out.println("request path:" + path);
		// Matcher matcher = renderJapidWithPattern.matcher(path);
		// if (matcher.matches()) {
		// String template = matcher.group(1);
		// JapidController.renderJapidWith(template);
		// }
	}

//	private void logDuration(String where) {
//		Request current = Request.current();
//		if (current == null)
//			return;
//		Map<String, Object> args = current.args;
//		Object whenRaw = args.get(WHEN_REQ_IN);
//		if (whenRaw != null && whenRaw instanceof Long) {
//			long start = (Long)whenRaw;
//			long now = System.nanoTime();
//			String timeBeforeAction = StringUtils.durationInMsFromNanos(start, now);
//			JapidFlags.debug("Time required for the request to reach " + where + ": " + timeBeforeAction + "ms.");
//		}
//	}

	String dumpRequest = null;
	private ArrayList<Route> recentAddedRoutes;
	private String ctxPath;

	@Override
	public boolean rawInvocation(Request req, Response response) throws Exception {
		req.args.put(WHEN_REQ_IN, System.nanoTime());

		Mode mode = Play.mode;
		if (mode == Mode.DEV) {
			String path = req.path;
			if (path.endsWith("_japidgen")) {
				try {
					beforeDetectingChanges();
					JapidPlayRenderer.gen();
				} catch (Exception e) {
				}
				response.out.write("OK".getBytes());
				return true;
			} else if (path.endsWith("_japidregen")) {
				try {
					JapidCommands.regen();
					JapidPlayRenderer.regen();
				} catch (Exception e) {
				}
				response.out.write("OK".getBytes());
				return true;
			}
		}

		if (dumpRequest != null && dumpRequest.length() > 0) {
			if ("yes".equals(dumpRequest) || "true".equals(dumpRequest)) {
				System.out.println("---->>" + req.method + " : " + req.url + " [" + req.action + "]");
				// System.out.println("request.controller:" +
				// current.controller);
			} else if ("false".equals(dumpRequest) || "no".equals(dumpRequest)) {
				// do nothing
			} else if (req.url.matches(dumpRequest)) {
				String querystring = req.querystring;
				if (querystring != null && querystring.length() > 0)
					querystring = "?" + querystring;
				else
					querystring = "";
				System.out.println("---->>" + req.method + " : " + req.url + querystring);
				String contentType = req.contentType;
				if (contentType == null || contentType.length() == 0) {
					contentType = "";
				}

				for (String k : req.headers.keySet()) {
					Header h = req.headers.get(k);
					System.out.println("... " + h.name + ":" + URLDecoder.decode(h.value(), "utf-8"));
				}
				// cookie is already in the headers
				// for (String ck : req.cookies.keySet()) {
				// Cookie c = req.cookies.get(ck);
				// System.out.println("... cookie --> " + c.name + ":" +
				// c.value);
				// }
				if ("POST".equals(req.method)) {
					if (contentType.contains("application/x-www-form-urlencoded")) {
						dumpReqBody(req, true);
					} else if (contentType.startsWith("text")) {
						dumpReqBody(req, false);
					} else if (contentType.contains("multipart/form-data")) {
						// cannot dump it, since it may contain binary
					} else if (contentType.contains("xml")) {
						dumpReqBody(req, false);
					} else if (contentType.contains("javascript")) {
						dumpReqBody(req, false);
					}
				}
			}

		}

		return false;

	}

	/**
	 * @param req
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	private void dumpReqBody(Request req, boolean urlDecode) throws IOException, UnsupportedEncodingException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		InputStream bodystream = req.body;
		int r = bodystream.read();
		while (r > -1) {
			bos.write(r);
			r = bodystream.read();
		}

		bodystream.close();

		String body = bos.toString("UTF-8");
		if (urlDecode)
			body = URLDecoder.decode(body, "UTF-8");
		System.out.println("... body -> " + body);
		req.body = new ByteArrayInputStream(bos.toByteArray());
	}

	/**
	 * this takes place before the flash and session save in the ActionInvoker
	 */
	@Override
	public void onActionInvocationResult(Result result) {
//		logDuration("onActionInvocationResult");
		Flash fl = Flash.current();
		if (RenderResultCache.shouldIgnoreCacheInCurrentAndNextReq()) {
			fl.put(RenderResultCache.READ_THRU_FLASH, "yes");
		} else {
			fl.remove(RenderResultCache.READ_THRU_FLASH);
			fl.discard(RenderResultCache.READ_THRU_FLASH);
		}

		// always reset the flag since the thread may be reused for another
		// request processing
		RenderResultCache.setIgnoreCacheInCurrentAndNextReq(false);
	}

	/**
	 * right after an action is invoked. See {@code ActionInvoker} Currently
	 * this code has no effect to the flash due to a bug in ActionInvoker where
	 * Flash save its state to cookie.
	 */
	@Override
	public void afterActionInvocation() {
//		logDuration("afterActionInvocation");
		JapidController.threadData.get().remove(ACTION_METHOD);
	}

	@Override
	public void detectChange() {
//		logDuration("detectChange");
	}

	/**
	 * this thing happens very early in the cycle. Neither Session nore Flash is
	 * restored yet.
	 */
	@Override
	public void beforeInvocation() {
//		logDuration("beforeInvocation");
	}

	@Override
	public void afterApplicationStart() {
//		logDuration("afterApplicationStart");
		getDumpRequest();
		setupInjectTemplateBorder();
	}

	@Override
	public void onApplicationStart() {
		JapidFlags.debug("JapidPlugin: clean japidCache");
		japidCache.clear();
		buildRoutesFromAnnotations();
	}

	@Override
	public void onEvent(String message, Object context) {

	}

	/**
	 * intercept a special url that renders a Japid template without going thru
	 * a controller.
	 * 
	 * The url format: (anything)/renderJapidWith/(template path from japidview,
	 * not included) e.g.
	 * 
	 * http://localhost:9000/renderJapidWith/templates/callPicka
	 * 
	 * will render the template "templates/callPicka.html" in the japidview
	 * package in the app dir.
	 * 
	 * TODO: add parameter support.
	 */
	@Override
	public void routeRequest(Request request) {
//		logDuration("routeRequest");
		if (Play.mode == Mode.DEV) {
			buildRoutesFromAnnotations();
		}

		if (request.path.endsWith("_listroutes")) {
			List<Route> routes = Router.routes;
			for (Route r : routes) {
				JapidFlags.info(r.toString());
			}
		}
	}

	/**
	 * auto routes gave higher priority in the route table.
	 * 
	 * @author Bing Ran (bing.ran@gmail.com)
	 */
	private void buildRoutesFromAnnotations() {
		if (!Play.classloader.currentState.equals(lastApplicationClassloaderState)) {
			JapidFlags.debug("reload auto route due to classloader state change");
			buildRoutes();
		} else if (Router.lastLoading != routesLoadingTime) {
			JapidFlags.debug("reload auto route due to router timestamp");
			buildRoutes();
		}
		this.ctxPath = Play.ctxPath;
		lastApplicationClassloaderState = Play.classloader.currentState;
		routesLoadingTime = Router.lastLoading;
	}

	private void buildRoutes() {

		List<Route> oldRoutes = Router.routes;
		List<Route> newRoutes = new ArrayList<Route>(oldRoutes.size());

		if (this.lastApplicationClassloaderState == Play.classloader.currentState && recentAddedRoutes != null
				&& this.ctxPath == Play.ctxPath) {
			JapidFlags.debug("classloader state not changed. Use cached auto-routes.");
			newRoutes = new ArrayList<Route>(recentAddedRoutes);
		} else {
			// rebuild the dynamic route table
			long start = System.nanoTime();
			boolean ctxChanged = this.ctxPath != Play.ctxPath;

			for (ApplicationClass ac : Play.classes.all()) {
				// check cache
				RouterClass r = routerCache.get(ac.name);
				if (r != null && !ctxChanged && r.matchHash(ac.sigChecksum)) {
					// no change
					// JapidFlags.debug(ac.name + " has not changed. ");
				} else {
					Class<?> javaClass = ac.javaClass;
					if (javaClass != null  
							&& javaClass.getName().startsWith("controllers.")
							&& javaClass.getAnnotation(AutoPath.class) != null) {
						JapidFlags.debug("generate route for: " + ac.name);
						r = new RouterClass(javaClass, appPath, ac.sigChecksum);
					} else
						continue;
				}

				this.routerCache.put(ac.name, r);
				newRoutes.addAll(r.buildRoutes());
			}
			//
			JapidFlags.debug("rebuilding auto paths took(/ms): " + StringUtils.durationInMsFromNanos(start, System.nanoTime()));

			this.recentAddedRoutes = new ArrayList<Route>(newRoutes);
			this.lastApplicationClassloaderState = Play.classloader.currentState;
		}

		// copy fixed routes from the old route
		for (Iterator<Route> iterator = oldRoutes.iterator(); iterator.hasNext();) {
			Route r = iterator.next();
			if (r.routesFileLine != RouterMethod.AUTO_ROUTE_LINE) {
				newRoutes.add(r);
			}
		}
		Router.routes = newRoutes;
	}

	private ApplicationClassloaderState lastApplicationClassloaderState = null;
	private long routesLoadingTime = 0;
	private Map<String, RouterClass> routerCache = new HashMap<String, RouterClass>();

	private static Pattern renderJapidWithPattern = Pattern.compile(".*" + RENDER_JAPID_WITH + "/(.+)");

	/**
	 * on tomcat this is the sure way to get the ctxPath from Play. The servlet
	 * wrapper is the culprit.
	 */
	@Override
	public void onRoutesLoaded() {
		// if (!Play.standalonePlayServer) {
		appPath = Play.ctxPath;
		// System.out.println("reload auto route due to system route loaded.");
		buildRoutes();
		lastApplicationClassloaderState = Play.classloader.currentState;
		routesLoadingTime = Router.lastLoading;
		// }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see play.PlayPlugin#onInvocationException(java.lang.Throwable)
	 */
	@Override
	public void onInvocationException(Throwable e) {

	}

//	/* (non-Javadoc)
//	 * @see play.PlayPlugin#loadTemplate(play.vfs.VirtualFile)
//	 */
//	@Override
//	public Template loadTemplate(VirtualFile file) {
//		Template t = new JapidPlayTemplate() {
//			@Override
//			protected String internalRender(Map<String, Object> args) {
//				// TODO Auto-generated method stub
//				return super.internalRender(args);
//			}
//		};
//	}
}
