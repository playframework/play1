package cn.bran.japid.template;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import cn.bran.japid.rendererloader.RendererClass;
import cn.bran.japid.util.RenderInvokerUtils;

/**
 * a helper class. for hiding the template API from user eyes. not really needed
 * since the template invocation API is simple enough.
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * @deprecated don't use the play way of subclassing a controller. Use
 *             JapidRender directly anywhere.
 */
class JapidPlainController {
	private static final char DOT = '.';
	private static final String HTML = ".html";
	private static final String JAPIDVIEWS_ROOT = "japidviews";

	/**
	 * render an array of objects to a template defined by a Template class.
	 * 
	 * @param <T>
	 *            a sub-class type of JapidTemplateBaseWithoutPlay
	 * @param c
	 *            a sub-class of JapidTemplateBase
	 * @param args
	 *            arguments
	 */
	public static <T extends JapidTemplateBaseWithoutPlay> String renderWith(Class<T> c, Object... args) {
		checkJapidInit();

		if (JapidRenderer.isDevMode())
			return renderJapidWith(c.getName(), args);
		else
			try {
				return invokeRender(c, args);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
	}

	/**
	 * 
	 */
	private static void checkJapidInit() {
		if (!JapidRenderer.isInited()) {
			throw new RuntimeException("The Japid is not initialized. Please use JapidRender.init(...) to set it up.");
		}
	}

	/**
	 * @param <T>
	 * @param c
	 *            the Japid renderer class, to be used with reflection.
	 * @param args
	 * @return
	 * @throws NoSuchMethodException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private static <T extends JapidTemplateBaseWithoutPlay> String invokeRender(Class<T> c, Object... args) {
		int modifiers = c.getModifiers();
		if (Modifier.isAbstract(modifiers)) {
			throw new RuntimeException("Cannot init the template class since it's an abstract class: " + c.getName());
		}
		try {
			Constructor<T> ctor = c.getConstructor(StringBuilder.class);
			StringBuilder sb = new StringBuilder(8000);
			T t = ctor.newInstance(sb);
			String rr = (String) RenderInvokerUtils.render(t, args);
			return rr;
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Could not match the arguments with the template args.");
		} catch (InstantiationException e) {
			// e.printStackTrace();
			throw new RuntimeException("Could not instantiate the template object. Abstract?");
		} catch (Exception e) {
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException("Could not invoke the template object: " + e);
			// throw new RuntimeException(e);
		}
	}

	/**
	 * pickup the Japid renderer in the conventional location and render it.
	 * Positional match is used to assign values to parameters
	 * 
	 * @param objects
	 */
	protected static String render(Object... objects) {
		checkJapidInit();
		String action = template();
		return renderJapidWith(action, objects);
	}

	public static String renderJapidWith(String template, Object... args) {
		checkJapidInit();
		return getRenderResultWith(template, args);
	}

	protected static String template() {
		// the super.template() class uses current request object to determine
		// the caller and method to find the matching template
		// this won't work if the current method is called from another action.
		// let's fall back to use the stack trace to deduce the template.
		// String caller2 = StackTraceUtils.getCaller2();

		final StackTraceElement[] stes = new Throwable().getStackTrace();

		for (StackTraceElement st : stes) {
			String controller = st.getClassName();
			String action = st.getMethodName();
			Class<?> controllerClass;
			try {
				controllerClass = JapidPlainController.class.getClassLoader().loadClass(controller);
				if (controllerClass != null) {
					Class<?> superclass = controllerClass.getSuperclass();
					if (JapidPlainController.class.isAssignableFrom(superclass)) {
						String expr = controller + "." + action;
						return expr;
					}
				}
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * copies from the same method in the Java class. Removed the public
	 * requirement for easier chaining.
	 * 
	 * @param name
	 * @param clazz
	 * @return
	 */
	static Method findActionMethod(String name, Class clazz) {
		while (!clazz.getName().equals("java.lang.Object")) {
			for (Method m : clazz.getDeclaredMethods()) {
				if (m.getName().equalsIgnoreCase(name) /*
														 * &&
														 * Modifier.isPublic(m
														 * .getModifiers())
														 */) {
					return m;
				}
			}
			clazz = clazz.getSuperclass();
		}
		return null;
	}

	/**
	 * render parameters to the prescribed template and return the RenderResult
	 * 
	 * @param template
	 *            relative path from japidviews folder. if empty, use implicit
	 *            naming pattern to match the template
	 * @param args
	 */
	public static String getRenderResultWith(String template, Object... args) {
		checkJapidInit();

		if (template == null || template.length() == 0) {
			template = template();
		}

		if (template.endsWith(HTML)) {
			template = template.substring(0, template.length() - HTML.length());
		}

		String templateClassName = template.startsWith(JAPIDVIEWS_ROOT) ? template : JAPIDVIEWS_ROOT + File.separator
				+ template;

		templateClassName = templateClassName.replace('/', DOT).replace('\\', DOT);

		Class<? extends JapidTemplateBaseWithoutPlay> tClass = null;

		// tClass = JapidRenderer.getClass(templateClassName);
		//
		// if (tClass == null) {
		// String templateFileName = templateClassName.replace(DOT, '/') + HTML;
		// throw new
		// RuntimeException("Could not find a Japid template with the name of: "
		// + templateFileName);
		// } else {
		// // render(tClass, args);
		// String rr = invokeRender(tClass, args);
		// return rr;
		// }
		RendererClass rc = JapidRenderer.getFunctionalRendererClass(templateClassName);
		if (rc == null) {
			String templateFileName = templateClassName.replace(DOT, '/') + HTML;
			throw new RuntimeException("Could not find a Japid template with the name of: " + templateFileName);
		} else {
			String rr = invokeRender(rc, args);
			return rr;
		}

	}

	/**
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param rc
	 * @param args
	 * @return
	 */
	private static String invokeRender(RendererClass rc, Object[] args) {
		try {
			Method apply = rc.getApplyMethod();
			String rr = (String) RenderInvokerUtils.renderWithApply(apply, args);
			return rr;
		} catch (Exception e) {
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException("Could not invoke the template object: " + e);
		}
	}

}
