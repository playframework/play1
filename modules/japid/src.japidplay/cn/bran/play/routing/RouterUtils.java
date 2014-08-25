/**
 * 
 */
package cn.bran.play.routing;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import play.libs.F.Tuple;
import cn.bran.play.routing.HttpMethod.DELETE;
import cn.bran.play.routing.HttpMethod.GET;
import cn.bran.play.routing.HttpMethod.HEAD;
import cn.bran.play.routing.HttpMethod.OPTIONS;
import cn.bran.play.routing.HttpMethod.POST;
import cn.bran.play.routing.HttpMethod.PUT;

/**
 * @author bran
 * 
 */
public class RouterUtils {

	static String urlParamCapture = "\\{(.*?)\\}";
	static Pattern urlParamCaptureP = Pattern.compile(urlParamCapture);

	// old perhaps incorrect impl. was looking for subgroups
	// static List<String> findAllIn(Pattern p, String string) {
	// List<String> ret = new ArrayList<String>();
	// Matcher matcher = p.matcher(string);
	// while (matcher.find()) {
	// int c = matcher.groupCount();
	// for (int i = 1; i <= c; i++) {
	// String group = matcher.group(i);
	// ret.add(group);
	// }
	// }
	// return ret;
	// }
	//
	// find class level annotation
	/**
	 * find if the uri contains variables
	 * 
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param uri
	 * @param path
	 * @return true if it contains, false otherwise
	 */
	static boolean isResourcePath(String uri, String path) {
		String r = path.replaceAll(urlParamCapture, "(.*)");
		Pattern p = Pattern.compile(r);
		if (!RegMatch.findAllIn(p, uri).isEmpty())
			return true;
		else
			return false;
	}

	static Class<? extends Annotation> findHttpMethodAnnotation(String httpMethod) {
		if (httpMethod.equals("GET"))
			return GET.class;
		else if (httpMethod.equals("POST"))
			return POST.class;
		else if (httpMethod.equals("POST"))
			return POST.class;
		else if (httpMethod.equals("PUT"))
			return PUT.class;
		else if (httpMethod.equals("DELETE"))
			return DELETE.class;
		else if (httpMethod.equals("HEAD"))
			return HEAD.class;
		else if (httpMethod.equals("OPTIONS"))
			return OPTIONS.class;
		else
			return null;
	}

	static TargetClassWithPath findLongestMatch(Set<Class<?>> classes, play.mvc.Http.Request req,
			String appPath) {
		TargetClassWithPath ret = null;
		String rpath = req.path;
		for (Class<?> c : classes) {
			String path = appPath + prefixSlash(c.getAnnotation(AutoPath.class).value());
			if (isResourcePath(rpath, path)) {
				if (ret == null || path.length() > ret._2().length()) {
					ret = new TargetClassWithPath(c, path);
				}
			}
		}
		return ret;
	}

	/**
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param value
	 * @return
	 */
	private static String prefixSlash(String value) {
		return value.startsWith("/") ? value : "/" + value;
	}

//	static RouterClass findLongestMatch(List<RouterClass> classes, play.mvc.Http.Request reqHeader) {
//		RouterClass ret = null;
//		String rpath = reqHeader.path;
//		for (RouterClass c : classes) {
//			if (!RegMatch.findAllIn(c.absPathPatternForValues, rpath).isEmpty())
//				if (ret == null || c.absPath.length() > ret.absPath.length()) {
//					ret = c;
//				}
//		}
//		return ret;
//	}

//	static java.util.Set<Method> relevantMethods(Class<?> c, Class<? extends Annotation> a) {
//		return ReflectionUtils.getAllMethods(c, Predicates.and(ReflectionUtils.withAnnotation(a)));
//	}

	/**
	 * 
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param rootPath
	 * @param uri
	 * @param methods
	 * @param contentType
	 * @return a tuple of method and its param name-value map
	 */
	static Tuple<Method, Map<String, String>> findMethodAndGenerateContext(String rootPath, String uri,
			java.util.Set<Method> methods, String contentType) {
		// include rootPath only for non root slash class level @Path values or
		// if the uri is root slash
		String[] consumeTypes = new String[] { contentType };
		String rootPathPrefix = (rootPath == "/" && uri != "/") ? "" : rootPath;

		for (Method m : methods) {
//
//			Consumes consumes = m.getAnnotation(Consumes.class);
//			if (consumes != null) {
//				consumeTypes = consumes.value();
//			}
//
			boolean contained = false;
			for (String c : consumeTypes) {
				if (c.equals(contentType)) {
					contained = true;
					break;
				}
			}
			if (contained) {
				AutoPath p = m.getAnnotation(AutoPath.class);
				String mPath = p == null ? "" : prefixSlash(p.value());
				String fullMethodPath = (rootPathPrefix + mPath).replace("//", "/");

				// List<String> matches =
				// RegMatch.findAllIn(Pattern.compile(fullMethodPath), uri );
				// if (matches.size() > 0) {
				// matches = RegMatch.findAllIn(Pattern.compile(uri),
				// fullMethodPath);
				// if (matches.size() > 0) {
				// return new Tuple<Method, Map<String, String>>(m, new
				// HashMap<String, String>());
				// }
				// }

				// bran let's do exact match
				if (uri.equals(fullMethodPath)) {
					return new Tuple<Method, Map<String, String>>(m, new HashMap<String, String>());
				} else // any variables?
				if (fullMethodPath.contains("{") && fullMethodPath.contains("}")) {
					String combinedReg = fullMethodPath.replaceAll(urlParamCapture, "\\\\{(.*)\\\\}");// .r;
					Pattern r = Pattern.compile(combinedReg);
					List<RegMatch> rootParamNameMatches = RegMatch.findAllMatchesIn(r, fullMethodPath);
					List<String> rootParamNames = new ArrayList<String>();
					for (RegMatch rm : rootParamNameMatches) {
						rootParamNames.addAll(rm.subgroups);
					}

					combinedReg = fullMethodPath.replaceAll(urlParamCapture, "(.*)");
					r = Pattern.compile(combinedReg);
					List<RegMatch> rootParamValueMatches = RegMatch.findAllMatchesIn(r, uri);
					List<String> rootParamValues = new ArrayList<String>();
					for (RegMatch rm : rootParamValueMatches) {
						rootParamValues.addAll(rm.subgroups);
					}

					Map<String, String> methodMetaData = new java.util.HashMap<String, String>();
					int c = 0;
					for (String name : rootParamNames) {
						try {
							methodMetaData.put(name, rootParamValues.get(c++));
						} catch (Exception e) {
							methodMetaData.put(name, "");
						}
					}

					if (rootParamValues.size() > 0) {
						return new Tuple<Method, Map<String, String>>(m, methodMetaData);
					}
				}
			}
		}
		return null;
	}
	
//	static Result invokeMethod(Class<?> targetClass, Method method,
//			Map<String, Object> extractedArgs, play.mvc.Http.Request r) {
//		try {
//			Object[] argValues = new Object[0];
//			List<Object> argVals = new ArrayList<Object>();
//			Annotation[][] annos = method.getParameterAnnotations();
//
//			for (Annotation[] ans : annos) {
//				PathParam pathParam = null;
//				QueryParam queryParam = null;
//
//				for (Annotation an : ans) {
//					if (an instanceof PathParam)
//						pathParam = (PathParam) an;
//					else if (an instanceof QueryParam)
//						queryParam = (QueryParam) an;
//				}
//				if (pathParam != null) {
//					Object v = extractedArgs.get(pathParam.value());
//					if (v != null)
//						argVals.add(v);
//					else
//						throw new IllegalArgumentException("can not find annotation value for argument "
//								+ pathParam.value() + "in " + targetClass + "#" + method);
//				} else if (queryParam != null) {
//					Object v = extractedArgs.get(queryParam.value()); // assuming already extracted
//					argVals.add(v);
//				} else
//					throw new IllegalArgumentException(
//							"can not find an appropriate JAX-RC annotation for an argument for method:" + targetClass
//									+ "#" + method);
//			}
//			argValues = argVals.toArray(argValues);
//			return (play.mvc.results.Result) method.invoke(null, argValues);
//		} catch (InvocationTargetException cause) {
//			System.err.println("Exception occured while trying to invoke: " + targetClass.getName() + "#"
//					+ method.getName() + " with " + extractedArgs + " for uri:" + r.path);
//			throw new RuntimeException(cause.getCause());
//		} catch (Exception e) {
//			throw new RuntimeException(e.getCause());
//		}
//	}

//	// TODO: find out why ref.getTypesAnnotatedWith(classOf[Path]) is not
//	// working
//	static Set<Class<?>> classes(ClassLoader parentClassloader) {
//		Set<String> typesAnnotatedWith = JaxrsRouter.ref.getStore().getTypesAnnotatedWith(Path.class.getName());
//		Set<Class<?>> clazz = new HashSet<Class<?>>();
//		for (String t : typesAnnotatedWith) {
//			try {
//				clazz.add(Class.forName(t, true, parentClassloader));
//			} catch (ClassNotFoundException e) {
//				throw new RuntimeException(e);
//			}
//		}
//		return clazz;
//	}

}
