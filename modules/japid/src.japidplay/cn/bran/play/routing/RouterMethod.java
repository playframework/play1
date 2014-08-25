/**
 * 
 */
package cn.bran.play.routing;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import play.mvc.Router.Route;
import play.utils.Java;
import cn.bran.play.routing.HttpMethod.DELETE;
import cn.bran.play.routing.HttpMethod.GET;
import cn.bran.play.routing.HttpMethod.HEAD;
import cn.bran.play.routing.HttpMethod.OPTIONS;
import cn.bran.play.routing.HttpMethod.POST;
import cn.bran.play.routing.HttpMethod.PUT;

public class RouterMethod {
	/**
	 * 
	 */
	public static final int AUTO_ROUTE_LINE = -1;
	// false when an explicit AuthPath specified to action
	private boolean autoRouting = false;
	// the artificial url extension such as .html
	private String withExtension = "";

	private String pathPrefix;
	private List<Route> routes;
	private String pathEnding = "";

	/**
	 * 
	 * no method annotation means taking "any"
	 * 
	 * @param m
	 */
	public RouterMethod(Method m, String pathPrefix) {

		List<Route> routes = new ArrayList<Route>();

		this.pathPrefix = pathPrefix;
		Annotation[] annotations = m.getAnnotations();
		for (Annotation a : annotations) {
			if (a instanceof GET || a instanceof POST || a instanceof PUT || a instanceof DELETE || a instanceof HEAD
					|| a instanceof OPTIONS)
				httpMethodAnnotations.add(a);
			else if (a instanceof EndWith) {
				this.pathEnding = ((EndWith) a).value();
			}
		}
		meth = m;
		// Consumes consumes = m.getAnnotation(Consumes.class);
		// if (consumes != null) {
		// consumeTypes = consumes.value();
		// }

		// now parse the path spec
		AutoPath p = m.getAnnotation(AutoPath.class);
		if (p != null && p.value().length() > 0) {
			String value = p.value();
			pathSpec = pathPrefix + value;
		} else {
			// auto-routing mechanism:
			// 1. use method name as the first part
			this.autoRouting = true;
			pathSpec = pathPrefix + "." + m.getName();

		}

		// if GET and others, create a parameter list for the rest of the path
		// if POST presents, no parameter placeholder is added to the path
		// boolean containPOST = false;

		String paramNames = join(getMethodParamNames(m));

		String act = m.getDeclaringClass().getName() + "." + m.getName();
		if (act.startsWith("controllers."))
			act = act.substring("controllers.".length());

		if (httpMethodAnnotations.size() == 0) {
			if (autoRouting) {
				// automatically added a special post for POST
				// no params on path
				if (paramNames.length() > 0) {
					Route r = new Route();
					r.method = "POST";
					r.path = pathSpec; // no params, no post-fix.
					r.action = act;
					r.routesFile = "_autopath";
					r.routesFileLine = AUTO_ROUTE_LINE;
					r.compute();
					routes.add(r);
				}
				// the catch other
				Route r = new Route();
				r.method = "*";
				r.path = pathSpec + paramNames + pathEnding;
				r.action = act;
				r.routesFile = "_autopath";
				r.routesFileLine = AUTO_ROUTE_LINE;
				r.compute();
				routes.add(r);
			} else {
				Route r = new Route();
				r.method = "*";
				r.path = pathSpec;
				r.action = act;
				r.routesFile = "_autopath";
				r.routesFileLine = AUTO_ROUTE_LINE;
				r.compute();
				routes.add(r);
			}
		} else {
			for (Annotation an : httpMethodAnnotations) {
				Route r = new Route();
				r.method = an.annotationType().getSimpleName();
				r.action = act;
				r.routesFile = "_autopath";
				r.routesFileLine = AUTO_ROUTE_LINE;
				if (!autoRouting || an instanceof POST) {
					r.path = pathSpec;
				} else {
					r.path = pathSpec + paramNames + pathEnding;
				}
				r.compute();
				routes.add(r);
			}
		}

		pathSpecPattern = Pattern.compile(pathSpec.replaceAll(RouterClass.urlParamCapture, "\\\\{(.*)\\\\}"));

		this.routes = routes;
	}

	private String[] getMethodParamNames(Method m) {
		try {
			return Java.parameterNames(m);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param paramNames
	 * @param string
	 * @return
	 */
	private String join(String[] paramNames) {
		if (paramNames.length == 0)
			return "";
		else {
			String ret = "";
			for (String p : paramNames) {
				ret += "/{" + p + "}";
			}
			return ret;
		}
	}

	/**
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param string
	 */
	private static void error(String string) {
		throw new RuntimeException(string);
	}

	/**
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param c
	 * @param name
	 * @param vals
	 * @param type
	 * @return
	 */
	private Object convertArgType(int c, String name, String[] vals, Class<?> type) {
		Object val = null;
		int vlen = vals.length;
		if (type == Boolean[].class) {
			val = new Boolean[vlen];
			for (int i = 0; i < vlen; i++) {
				((Boolean[]) val)[i] = Boolean.valueOf(vals[i]);
			}
		} else if (type == boolean[].class) {
			val = new boolean[vlen];
			for (int i = 0; i < vlen; i++) {
				((boolean[]) val)[i] = Boolean.valueOf(vals[i]);
			}
		} else if (type == Byte[].class) {
			val = new Byte[vlen];
			for (int i = 0; i < vlen; i++) {
				((Byte[]) val)[i] = Byte.valueOf(vals[i]);
			}
		} else if (type == byte[].class) {
			val = new byte[vlen];
			for (int i = 0; i < vlen; i++) {
				((byte[]) val)[i] = Byte.valueOf(vals[i]);
			}
		} else if (type == Character[].class) {
			val = new Character[vlen];
			for (int i = 0; i < vlen; i++) {
				((Character[]) val)[i] = vals[i].charAt(0);
			}
		} else if (type == char[].class) {
			val = new char[vlen];
			for (int i = 0; i < vlen; i++) {
				((char[]) val)[i] = vals[i].charAt(0);
			}
		} else if (type == Double[].class) {
			val = new Double[vlen];
			for (int i = 0; i < vlen; i++) {
				((Double[]) val)[i] = Double.valueOf(vals[i]);
			}
		} else if (type == double[].class) {
			val = new double[vlen];
			for (int i = 0; i < vlen; i++) {
				((double[]) val)[i] = Double.valueOf(vals[i]);
			}
		} else if (type == Float[].class) {
			val = new Float[vlen];
			for (int i = 0; i < vlen; i++) {
				((Float[]) val)[i] = Float.valueOf(vals[i]);
			}
		} else if (type == float[].class) {
			val = new float[vlen];
			for (int i = 0; i < vlen; i++) {
				((float[]) val)[i] = Float.valueOf(vals[i]);
			}
		} else if (type == Integer[].class) {
			val = new Integer[vlen];
			for (int i = 0; i < vlen; i++) {
				((Integer[]) val)[i] = Integer.valueOf(vals[i]);
			}
		} else if (type == int[].class) {
			val = new int[vlen];
			for (int i = 0; i < vlen; i++) {
				((int[]) val)[i] = Integer.valueOf(vals[i]);
			}
		} else if (type == Long[].class) {
			val = new Long[vlen];
			for (int i = 0; i < vlen; i++) {
				((Long[]) val)[i] = Long.valueOf(vals[i]);
			}
		} else if (type == long[].class) {
			val = new long[vlen];
			for (int i = 0; i < vlen; i++) {
				((long[]) val)[i] = Long.valueOf(vals[i]);
			}
		} else if (type == Short[].class) {
			val = new Short[vlen];
			for (int i = 0; i < vlen; i++) {
				((Short[]) val)[i] = Short.valueOf(vals[i]);
			}
		} else if (type == short[].class) {
			val = new short[vlen];
			for (int i = 0; i < vlen; i++) {
				((short[]) val)[i] = Short.valueOf(vals[i]);
			}
		} else if (type == String[].class) {
			val = vals;
		} else {
			throw new RuntimeException(
					"this version supports capturing primitive parameters, their object wrappers, strings or array of. This param is not of primitive type: "
							+ meth.getName() + ":" + c + "(0-based)");
		}
		return val;

	}

	private Object convertArgType(int c, String name, String value, Class<?> type) {
		Object val = null;
		if (type == Boolean.class || type == byte.class) {
			val = Boolean.valueOf(value);
		} else if (type == Byte.class || type == byte.class) {
			val = Byte.valueOf(value);
		} else if (type == Character.class || type == char.class) {
			if (value.length() != 1) {
				throw new IllegalArgumentException("cannot convert to a character: (" + name + ")" + value);
			}
			val = value.charAt(0);
		} else if (type == Double.class || type == double.class) {
			val = Double.valueOf(value);
		} else if (type == Float.class || type == float.class) {
			val = Float.valueOf(value);
		} else if (type == Integer.class || type == int.class) {
			val = Integer.valueOf(value);
		} else if (type == Long.class || type == long.class) {
			val = Long.valueOf(value);
		} else if (type == Short.class || type == short.class) {
			val = Short.valueOf(value);
		} else if (type == String.class) {
			val = value;
		} else {
			throw new RuntimeException(
					"this version supports capturing primitive parameters, their object wrappers or strings. This param is not of primitive type: "
							+ meth.getName() + ":" + c + "(0-based)");
		}
		return val;
	}

	List<Annotation> httpMethodAnnotations = new ArrayList<Annotation>();
	Method meth;
	String pathSpec;
	Pattern pathSpecPattern;
	public Pattern valueExtractionPattern;
	String produce;
	String[] consumeTypes = new String[] {};
	List<ParamSpec> paramSpecList = new ArrayList<ParamSpec>();

	/**
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param contentType
	 * @return
	 */
	public boolean containsConsumeType(String contentType) {
		if (consumeTypes.length == 0) {
			return true;
		} else {
			for (String c : consumeTypes) {
				if (c.equals(contentType))
					return true;
			}
		}
		return false;
	}

	/**
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param uri
	 * @return
	 */
	public boolean matchURI(String uri) {
		if (pathSpec.equals(uri))
			return true;
		else
			return valueExtractionPattern.matcher(uri).matches();
	}

	public boolean supportHttpMethod(String ms) {
		Class<? extends Annotation> httpMethodClass = RouterUtils.findHttpMethodAnnotation(ms.toUpperCase());
		if (httpMethodAnnotations.size() == 0)
			return true; // take any

		for (Annotation a : httpMethodAnnotations) {
			if (httpMethodClass.isInstance(a))
				return true;
		}
		return false;
	}

	/**
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @return
	 */
	public List<Route> buildRoutes() {
		// List<Route> routes = new ArrayList<Route>();
		// for (Annotation an : httpMethodAnnotations) {
		// Class<? extends Annotation> annotationType = an.annotationType();
		// String methName = annotationType.getSimpleName();
		// try {
		// String[] paramNames = Java.parameterNames(meth);
		// // String path =
		// } catch (Exception e) {
		// e.printStackTrace();
		// throw new RuntimeException(e);
		// }
		// }
		return this.routes;
	}
}