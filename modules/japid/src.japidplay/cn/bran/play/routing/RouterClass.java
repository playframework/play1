/**
 * 
 */
package cn.bran.play.routing;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import play.libs.F.Tuple;
import play.mvc.Http.Header;
import play.mvc.Router.Route;

/**
 * @author bran
 * 
 */
public class RouterClass {
	/**
	 * 
	 */
	private static final String CONTROLLERS = "controllers.";
	String absPath;

	List<String> routeTable = new ArrayList<String>();

	static String urlParamCapture = "\\{(.*?)\\}";
	static Pattern urlParamCaptureP = Pattern.compile(urlParamCapture);

	// the signature hash of the application class this class has been derived
	// from.
	private int applicationClassHash = 0;

	/**
	 * @param cl
	 */
	public RouterClass(Class<?> cl, String appPath, int sigHash) {
		this.applicationClassHash = sigHash;
		if (appPath == null)
			appPath = "";
		clz = cl;
		AutoPath anno = cl.getAnnotation(AutoPath.class);
		if (anno == null)
			path = "";
		else
			path = anno.value();

		if (path.length() == 0) {
			// auto-routing. using the class full name minus the "controller."
			// part as the path
			String cname = cl.getName();
			if (cname.startsWith(CONTROLLERS))
				cname = cname.substring(CONTROLLERS.length());
			path = cname;
		}
		if (appPath.endsWith("/"))
			absPath = appPath + path;
		else
			absPath = appPath + (path.startsWith("/") ? path : "/" + path);

		Method[] dms = cl.getDeclaredMethods();
		
		Method[] allMethods = cl.getDeclaredMethods();
		for (Method m : allMethods) {
			int modifiers = m.getModifiers();
			if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers) ) {
				routerMethods.add(new RouterMethod(m, absPath));
			}
		}
	}

	// public Tuple<Method,Object[]>
	// findMethodAndGenerateArgs(play.mvc.Http.Request r) {
	// String uri = r.path;
	//
	// String contentType = "";
	// Header ct = r.headers.get("Content-Type");
	// if (ct != null)
	// contentType = ct.value();
	//
	// for (RouterMethod m : routerMethods) {
	// if (m.containsConsumeType(contentType)
	// && m.supportHttpMethod(r.method)
	// && m.matchURI(uri)) {
	// return new Tuple<Method, Object[]>(m.meth, m.extractArguments(r));
	// }
	// }
	// return null;
	// }

	Class<?> clz;
	private String path;
	List<RouterMethod> routerMethods = new ArrayList<RouterMethod>();

	/**
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @return
	 */
	public List<Route> buildRoutes() {
		 List<Route> list = new ArrayList<Route>();
		 for (RouterMethod rm : routerMethods) {
			 list.addAll(rm.buildRoutes());
		 }
		 return list;
	}

	/**
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param sigChecksum
	 * @return
	 */
	public boolean matchHash(int sigChecksum) {
		return sigChecksum == applicationClassHash;
	}
}
