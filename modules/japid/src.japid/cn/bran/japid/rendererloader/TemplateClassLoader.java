package cn.bran.japid.rendererloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import cn.bran.japid.template.JapidRenderer;
import cn.bran.japid.template.JapidTemplateBaseWithoutPlay;
import cn.bran.japid.template.RenderResult;
import cn.bran.japid.util.JapidFlags;
import cn.bran.japid.util.RenderInvokerUtils;

/**
 * The template class loader that detects changes and recompile on the fly.
 * 
 * 1. whenever changes detected, clear the global class cache. 2. Only redefine
 * the class to load, which will lead to define all the dependencies. All the
 * dependencies must be defined by the same class classloader or
 * InvalidAccessException. 3. The main program will call the loadClass once for
 * each of the classes defined in one classloader.
 * 
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * 
 */
public class TemplateClassLoader extends ClassLoader {
	// the per classloader class cache
	private Map<String, Class<?>> localClasses = new ConcurrentHashMap<String, Class<?>>();
	private ClassLoader parentClassLoader;

	public TemplateClassLoader(ClassLoader cl) {
		super(TemplateClassLoader.class.getClassLoader());
		this.parentClassLoader = cl;
	}

	protected ClassLoader getParentClassLoader() {
		return parentClassLoader;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		// System.out.println("TemplateClassLoader.loadClass(): " + name);
		if (!name.startsWith(JapidRenderer.JAPIDVIEWS)) {
			try {
				Class<?> cls = getParentClassLoader().loadClass(name);
				return cls;
			} catch (ClassNotFoundException e) {
				return super.loadClass(name);
			}
		}

		String oid = "[TemplateClassLoader@" + Integer.toHexString(hashCode()) + "]";

		RendererClass rc = getJapidRendererClassWrapper(name);
		if (rc.getLastUpdated() > 0) {
			Class<?> cla = localClasses.get(name);
			if (cla != null) {
				return cla;
			}
		}

		// System.out.println("[TemplateClassLoader] loading: " + name);

		byte[] bytecode = rc.getBytecode();

		if (bytecode == null) {
			throw new RuntimeException(oid + " could not find the bytecode for: " + name);
		}

		// the defineClass method will load the classes of the dependency
		// classes.
		Class<? extends JapidTemplateBaseWithoutPlay> cl = (Class<? extends JapidTemplateBaseWithoutPlay>) defineClass(
				name, bytecode, 0, bytecode.length);
		rc.setClz(cl);
		JapidFlags.debug("redefined: " + rc.getClassName());
		// extract the apply(...)
		long count = 0; 
				
		for (Method m : cl.getDeclaredMethods()) {
			if (m.getName().equals("apply") && Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers())) {
				rc.setApplyMethod(m);
				count++;
			}
		}

		if (count == 0 && !name.contains("$")) {
			if (!Modifier.isAbstract(cl.getModifiers()))
				JapidFlags.warn("could not find the apply() with japid class: " + name);
		}

		localClasses.put(name, cl);
		rc.setLastUpdated(1);// System.currentTimeMillis();
		// if (JapidFlags.verbose)
		// System.out.println(oid + "class redefined from bytecode: " + name);
		return cl;

	}

	protected RendererClass getJapidRendererClassWrapper(String name) {
		return JapidRenderer.classes.get(name);
	}

	/**
	 * Search for the byte code of the given class from the current classpath
	 */
	protected byte[] getClassDefinition(String name) {
		// System.out.println("TemplateClassLoader.getClassDefinition(): " +
		// name);
		// throw new
		// RuntimeException("xxx: I'm not sure this is the right logic. Fix me please! -- "
		// + name);
		name = name.replace(".", "/") + ".class";
		InputStream is = getResourceAsStream(name);
		if (is == null) {
			// System.out.println("TemplateClassLoader.getClassDefinition()--nothing: "
			// + name);
			return null;
		}
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int count;
			while ((count = is.read(buffer, 0, buffer.length)) > 0) {
				os.write(buffer, 0, count);
			}
			// System.out.println("TemplateClassLoader.getClassDefinition()==got byte code: "
			// + name);
			return os.toByteArray();
		} catch (Exception e) {
			System.out.println("TemplateClassLoader.getClassDefinition(): exception: " + e);
			throw new RuntimeException(e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}