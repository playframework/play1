package cn.bran.play.rendererloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.libs.IO;

import cn.bran.japid.rendererloader.RendererClass;
import cn.bran.japid.rendererloader.TemplateClassLoader;
import cn.bran.japid.template.JapidRenderer;
import cn.bran.japid.template.JapidTemplateBaseWithoutPlay;
import cn.bran.japid.template.RenderResult;
import cn.bran.japid.util.JapidFlags;
import cn.bran.japid.util.RenderInvokerUtils;
import cn.bran.play.JapidPlayRenderer;

/**
 * The template class loader that detects changes and recompile on the fly and
 * use play's class loader to load classes referenced in the templates, such as
 * the model classes, controllers, etc.
 * 
 * 1. whenever changes detected, clear the global class cache. 2. Only redefine
 * the class to load, which will lead to define all the dependencies. All the
 * dependencies must be defined by the same class classloader or
 * InvalidAccessException. 3. The main program will call the loadClass once for
 * each of the classes defined in one classloader.
 * 
 * 
 * @author Bing Ran<bing.ran@gmail.com>
 * 
 */
public class TemplateClassLoaderWithPlay extends TemplateClassLoader {

//	public TemplateClassLoaderWithPlay() {
//		super(TemplateClassLoaderWithPlay.class.getClassLoader());
//	}

	public TemplateClassLoaderWithPlay(ClassLoader cl) {
		super(cl);
	}

//	@Override
//	protected ClassLoader getParentClassLoader() {
//		return play.Play.classloader;
//	}

	@Override
	protected RendererClass getJapidRendererClassWrapper(String name) {
		return JapidPlayRenderer.japidClasses.get(name);
	}

	@Override
	protected byte[] getClassDefinition(String name) {
		ApplicationClass applicationClass = play.Play.classes.getApplicationClass(name);
		if (applicationClass != null) {
			if (applicationClass.javaByteCode != null) {
				return applicationClass.javaByteCode;
			} else {
//				JapidFlags.log("Warning: no bytecode found for the applicationClass: " + name );
				// some hack. Due to a bug in Play, the bytecode of pre-compiled
				// classes are not set on the application class.
				// let's see if we can recover it.
				if (Play.usePrecompiled) {
//					JapidFlags.log("in pre-compiled mode. try to recover it.");
					try {
						File file = Play.getFile("precompiled/java/" + name.replace(".", "/") + ".class");
						if (!file.exists()) {
							return null;
						}
						byte[] code = IO.readContent(file);
						if (code != null && code.length > 0) {
							applicationClass.javaByteCode = code;
							return code;
						}
					} catch (Exception e) {
						JapidFlags.error("exception raised while trying to recover the bytecode: " + e);
					}
					JapidFlags.warn("could not recover it. It might be a bug.");
				}
				
				return null;
			}
		} else
			return super.getClassDefinition(name);
	}
}