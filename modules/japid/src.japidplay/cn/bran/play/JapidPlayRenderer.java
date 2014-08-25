package cn.bran.play;

import japidviews.error500ForPlay;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import play.Play;
import play.Play.Mode;
import play.classloading.ApplicationClassloader;
import play.classloading.ApplicationClassloaderState;
import play.data.validation.Validation;
import play.exceptions.CompilationException;
import play.templates.JavaExtensions;
import play.vfs.VirtualFile;
import cn.bran.japid.compiler.JapidCompilationException;
import cn.bran.japid.compiler.TranslateTemplateTask;
import cn.bran.japid.exceptions.JapidTemplateException;
import cn.bran.japid.rendererloader.RendererClass;
import cn.bran.japid.rendererloader.RendererCompiler;
import cn.bran.japid.rendererloader.TemplateClassLoader;
import cn.bran.japid.template.JapidRenderer;
import cn.bran.japid.template.JapidTemplateBaseWithoutPlay;
import cn.bran.japid.template.RenderResult;
import cn.bran.japid.util.DirUtil;
import cn.bran.japid.util.JapidFlags;
import cn.bran.japid.util.RenderInvokerUtils;
import cn.bran.japid.util.StackTraceUtils;
import cn.bran.play.rendererloader.TemplateClassLoaderWithPlay;
import cn.bran.play.util.PlayDirUtil;

/**
 * 
 * @author bran
 *
 */
public class JapidPlayRenderer {

	public static JapidTemplateBaseWithoutPlay getRenderer(String name) {
		Class<? extends JapidTemplateBaseWithoutPlay> c = getTemplateClass(name);
		try {
			return c.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static ApplicationClassloader playClassloader;
	private static boolean classesInited = false;
	private static ApplicationClassloaderState lastPlayClassLoaderState;

	/**
	 * Get a newly loaded class for the template renderer
	 * 
	 * @param name
	 * @return
	 */
	public static Class<? extends JapidTemplateBaseWithoutPlay> getTemplateClass(String name) {
		refreshClasses(/* name */);

		RendererClass rc = japidClasses.get(name);
		if (rc == null)
			throw new RuntimeException("Japid template class not found: " + name);
		else {
			if (isDevMode() && playClassloaderChanged()) {
				// always clear the mark to redefine all
				for (String c : japidClasses.keySet()) {
					japidClasses.get(c).setLastUpdated(0);
				}
				initJapidClassLoader();
			} else {
				if (rc.getLastUpdated() > 0 && rc.getClz() != null) {
					return rc.getClz();
				}
			}
		}

		// do I need to new instance of TemplateClassLoader for each invocation?
		// likely...
		// XXX why new instance each time?
//		TemplateClassLoaderWithPlay classReloader = new TemplateClassLoaderWithPlay(Play.classloader);

		try {
			return (Class<? extends JapidTemplateBaseWithoutPlay>) japidClassLoader.loadClass(name);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

	}

	private static void resetAllRenderClassUpdatedTime() {
		// always clear the mark to reload all
//		japidClasses.forEach((k, v )-> v.setLastUpdated(0));
		for (String c : japidClasses.keySet()) {
			japidClasses.get(c).setLastUpdated(0);
		}
	}

	/**
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @return
	 */
	private static boolean playClassloaderChanged() {
		if (play.Play.classloader != playClassloader || lastPlayClassLoaderState == null
				|| lastPlayClassLoaderState != play.Play.classloader.currentState) {
			playClassloader = play.Play.classloader;
			lastPlayClassLoaderState = play.Play.classloader.currentState;
			return true;
		} else
			return false;
	}

	static boolean timeToRefresh() {
		if (!isDevMode())
			return false;

		long now = System.currentTimeMillis();
		if (now - lastRefreshed > refreshInterval) {
			lastRefreshed = now;
			return true;
		} else
			return false;

	}

	static synchronized void refreshClasses() {
		if (classesInited) {
			if (!timeToRefresh())
				return;
		} else {
			JapidFlags.info("Japid scripts not loaded yet. Initializing them...");
		}

		try {
			// PlayDirUtil.mkdir(templateRoot);
			// find out all removed classes
			List<String> allTemps = isDevMode() ? DirUtil.getAllTemplateFiles(new File(defaultTemplateRoot)) : DirUtil
					.getAllTemplateFilesJavaFiles(new File(defaultTemplateRoot));
			Set<String> currentClassesOnDir = createNameSet(allTemps);
			Set<String> allNames = new HashSet<String>(currentClassesOnDir);

			Set<String> keySet = japidClasses.keySet();
			allNames.removeAll(keySet); // got new templates

			removeRemoved(currentClassesOnDir, keySet);

			for (String c : allNames) {
				RendererClass rc = newRendererClass(c);
				japidClasses.put(c, rc);
			}
			// now all the class set size is up to date

			// now update any Java source code
			List<File> gen = gen(defaultTemplateRoot);

			// this would include both new and updated java
			Set<String> updatedClasses = new HashSet<String>();
			if (gen.size() > 0) {
				for (File f : gen) {
					String className = getClassName(f);
					updatedClasses.add(className);
					RendererClass rendererClass = japidClasses.get(className);
					if (rendererClass == null) {
						// this should not happen, since
						throw new RuntimeException("any new class names should have been in the classes container: "
								+ className);
						// rendererClass = newRendererClass(className);
						// classes.put(className, rendererClass);
					}

					JapidRenderer.setSources(rendererClass, f);
					removeInnerClasses(className);
					cleanClassHolder(rendererClass);
				}
			}

			// find all render class without bytecode
			for (Iterator<String> i = japidClasses.keySet().iterator(); i.hasNext();) {
				String k = i.next();
				RendererClass rc = japidClasses.get(k);
				if (rc.getSourceCode() == null) {
					if (!rc.getClassName().contains("$")) {
						String pathname = defaultTemplateRoot + sep + k.replace(".", sep);
						File f = new File(pathname + ".java");
						JapidRenderer.setSources(rc, f);
						cleanClassHolder(rc);
						updatedClasses.add(k);
					} else {
						rc.setLastUpdated(0);
					}
				} else {
					if (rc.getBytecode() == null) {
						cleanClassHolder(rc);
						updatedClasses.add(k);
					}
				}
			}

			// compile all
			if (updatedClasses.size() > 0) {
				String[] names = new String[updatedClasses.size()];
				int i = 0;
				for (String s : updatedClasses) {
					names[i++] = s;
				}
				long t = System.currentTimeMillis();
				// newly compiled class bytecode bodies are set in the global
				// classes set ready for defining
				compiler.compile(names);
				
				initJapidClassLoader();
				for (RendererClass rc :  japidClasses.values()) {
					try {
						if (isDevMode())
							rc.setClz(null); // to enable JIT loading in DEV mode
						else
							rc.setClz((Class<JapidTemplateBaseWithoutPlay>) japidClassLoader.loadClass(rc.getClassName()));
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				}
				
				howlong("compile/load time for " + names.length + " classe(s)", t);

				classesInited = true;
			}
		} catch (JapidCompilationException e) {
			if (presentErrorInHtml)
				throw e;

			String tempName = e.getTemplateName();
			if (tempName.startsWith(defaultTemplateRoot)) {
			} else {
				tempName = defaultTemplateRoot + File.separator + tempName;
			}
			VirtualFile vf = VirtualFile.fromRelativePath(tempName);
			CompilationException ce = new CompilationException(vf, "\"" + e.getMessage() + "\"", e.getLineNumber(), 0,
					0);
			throw ce;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void initJapidClassLoader() {
		japidClassLoader = new TemplateClassLoaderWithPlay(Play.classloader);
	}

	public static void removeInnerClasses(String className) {
		for (Iterator<String> i = japidClasses.keySet().iterator(); i.hasNext();) {
			String k = i.next();
			if (k.startsWith(className + "$")) {
				i.remove();
			}
		}
	}

	/**
	 * @param currentClassesOnDir
	 * @param keySet
	 */
	public static void removeRemoved(Set<String> currentClassesOnDir, Set<String> keySet) {
		// need to consider inner classes
		// keySet.retainAll(currentClassesOnDir);

		for (Iterator<String> i = keySet.iterator(); i.hasNext();) {
			String k = i.next();
			int q = k.indexOf('$');
			if (q > 0) {
				k = k.substring(0, q);
			}
			if (!currentClassesOnDir.contains(k)) {
				i.remove();
			}
		}
	}

	// <classname RendererClass>
	public final static Map<String, RendererClass> japidClasses = new ConcurrentHashMap<String, RendererClass>();
	public static TemplateClassLoaderWithPlay japidClassLoader;

	public static TemplateClassLoaderWithPlay getCrlr() {
		return japidClassLoader;
	}

	public static RendererCompiler compiler;
	public static String defaultTemplateRoot = "japidroot";
	public static final String JAPIDVIEWS = "japidviews";
	public static String sep = File.separator;
	public static String japidviews = defaultTemplateRoot + sep + JAPIDVIEWS + sep;
	// such as java.utils.*
	public static List<String> importlines = new ArrayList<String>();
	public static int refreshInterval;
	public static long lastRefreshed = 0;
	// private static boolean inited;

	private static boolean usePlay = true;

	// to override Play's mode when set
	private static Mode mode;
	private static boolean presentErrorInHtml = true;

	public static Mode getMode() {
		if (mode == null)
			return play.Play.mode;
		else
			return mode;
	}

	static void howlong(String string, long t) {
		JapidFlags.debug(string + ":" + (System.currentTimeMillis() - t) + "ms");
	}

	/**
	 * @param rendererClass
	 */
	static void cleanClassHolder(RendererClass rendererClass) {
		rendererClass.setBytecode(null);
		rendererClass.setClz(null);
		rendererClass.setLastUpdated(0);
	}

	static Set<String> createNameSet(List<String> allHtml) {
		// the names start with template root
		Set<String> names = new HashSet<String>();
		for (String f : allHtml) {
			names.add(getClassName(new File(f)));
		}
		return names;
	}

	static String getSourceCode(String k) {
		String pathname = defaultTemplateRoot + sep + k.replace(".", sep);
		File f = new File(pathname + ".java");
		return readSource(f);
	}

	/**
	 * @param c
	 * @return
	 */
	static RendererClass newRendererClass(String c) {
		RendererClass rc = new RendererClass();
		rc.setClassName(c);
		// the source code of the Java file might not be available yet
		// rc.setSourceCode(getSouceCode(c));
		rc.setLastUpdated(0);
		return rc;
	}

	public static String readSource(File f) {
		try {
			FileInputStream fis = new FileInputStream(f);
			BufferedInputStream bis = new BufferedInputStream(fis);
			BufferedReader br = new BufferedReader(new InputStreamReader(bis, "UTF-8"));
			StringBuilder b = new StringBuilder();
			String line = null;
			while ((line = br.readLine()) != null) {
				b.append(line + "\n");
			}
			br.close();
			return b.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	static String getClassName(File f) {
		String path = f.getPath();
		String substring = path.substring(path.indexOf(JAPIDVIEWS));
		substring = substring.replace('/', '.').replace('\\', '.');
		substring = DirUtil.mapSrcToJava(substring);
		if (substring.endsWith(".java")) {
			substring = substring.substring(0, substring.length() - 5);
		} else {
			substring = DirUtil.mapSrcToJava(substring);
		}
		return substring;
	}

	/**
	 * set the interval to check template changes.
	 * 
	 * @param i
	 *            the interval in seconds. Set it to {@link Integer.MAX_VALUE}
	 *            to effectively disable refreshing
	 */
	static void setRefreshInterval(int i) {
		refreshInterval = i * 1000;
	}

	static public void setTemplateRoot(String root) {
		if (new File(root).isAbsolute())
			defaultTemplateRoot = root;
		else
			defaultTemplateRoot = Play.applicationPath + sep + root;
		japidviews = defaultTemplateRoot + sep + JAPIDVIEWS + sep;
		JapidFlags.info("japid root is set to: " + defaultTemplateRoot);
	}

	/**
	 * The entry point for the command line tool japid.bat and japid.sh
	 * 
	 * The "gen" and "regen" are probably the most useful ones.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			String arg0 = args[0];

			setTemplateRoot(".");
			if ("gen".equals(arg0)) {
				gen(defaultTemplateRoot);
			} else if ("regen".equals(arg0)) {
				regen(defaultTemplateRoot);
			} else if ("clean".equals(arg0)) {
				delAllGeneratedJava(getJapidviewsDir(defaultTemplateRoot));
			} else if ("mkdir".equals(arg0)) {
				mkdir(defaultTemplateRoot);
			} else if ("changed".equals(arg0)) {
				changed(japidviews);
			} else {
				System.err.println("help:  optionas are: gen, regen, mkdir and clean");
			}
		} else {
			System.err.println("help:  optionas are: gen, regen, mkdir and clean");
		}
	}

	private static void changed(String root) {
		List<File> changedFiles = DirUtil.findChangedSrcFiles(new File(root));
		for (File f : changedFiles) {
			System.out.println("changed: " + f.getPath());
		}

	}

	/**
	 * create the basic layout: app/japidviews/_javatags app/japidviews/_layouts
	 * app/japidviews/_tags
	 * 
	 * then create a dir for each controller. //TODO
	 * 
	 * @throws IOException
	 * 
	 */
	static List<File> mkdir(String root) throws IOException {
		return PlayDirUtil.mkdir(root);
	}

	/**
	 * @param root
	 * @return
	 */
	private static String getJapidviewsDir(String root) {
		return root + sep + JAPIDVIEWS + sep;
	}

	static void regen() throws IOException {
		regen(defaultTemplateRoot);
	}

	public static void regen(String root) throws IOException {
		delAllGeneratedJava(getJapidviewsDir(root));
		gen(root);
	}

	static void delAllGeneratedJava(String pathname) {
		String[] javas = DirUtil.getAllFileNames(new File(pathname), new String[] { "java" });

		for (String j : javas) {
			log("removed: " + pathname + j);
			boolean delete = new File(pathname + File.separatorChar + j).delete();
			if (!delete)
				throw new RuntimeException("file was not deleted: " + j);
		}
		// log("removed: all none java tag java files in " +
		// JapidPlugin.JAPIDVIEWS_ROOT);
	}

	/**
	 * update the java files from the html files, for the changed only
	 * 
	 * @throws IOException
	 */
	static List<File> gen(String packageRoot) throws IOException {
		List<File> changedFiles = reloadChanged(packageRoot);
		if (changedFiles.size() > 0) {
			// for (File f : changedFiles) {
			// // log("updated: " + f.getName().replace("html", "java"));
			// }
		} else {
			// log("All Japid template files are synchronized.");
			if (JapidFlags.verbose)
				System.out.print(":");
		}

		if (isDevMode())
			rmOrphanJava(packageRoot);
		return changedFiles;
	}

	/**
	 * @param root
	 *            the package root "/"
	 * @return the updated Java files.
	 */
	static List<File> reloadChanged(String root) {
		try {
			mkdir(root);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		TranslateTemplateTask t = new TranslateTemplateTask();
		t.setUsePlay(usePlay);
		File rootDir = new File(root);
		t.setPackageRoot(rootDir);
		t.setInclude(new File(japidviews));
		if (DirUtil.hasLayouts(root))
			t.addImport("japidviews._layouts.*");
		if (DirUtil.hasJavaTags(root))
			t.addImport("japidviews._javatags.*");
		if (DirUtil.hasTags(root))
			t.addImport("japidviews._tags.*");

		for (String imp : importlines) {
			t.addImport(imp);
		}

		// since we're bound together with play:
		t.importStatic(JapidPlayAdapter.class);
		t.importStatic(Validation.class);
		t.importStatic(JavaExtensions.class);
		t.addImport("models.*");
		t.addImport("controllers.*");
		t.addImport(play.mvc.Scope.class.getName() + ".*");
		t.addImport(play.i18n.Messages.class);
		t.addImport(play.i18n.Lang.class);
		t.addImport(play.mvc.Http.class.getName() + ".*");
		t.addImport(Validation.class.getName());
		t.addImport(play.data.validation.Error.class.getName());
		List<String> javatags = DirUtil.scanJavaTags(root);
		for (String f : javatags) {
			t.addImport("static " + f + ".*");
		}

		t.execute();
		// List<File> changedFiles = t.getChangedFiles();
		return t.getChangedTargetFiles();
		// return changedFiles;
	}

	/**
	 * get all the java files in a dir with the "java" removed
	 * 
	 * @return
	 */
	static File[] getAllJavaFilesInDir(String root) {
		// from source files only
		String[] allFiles = DirUtil.getAllFileNames(new File(root), new String[] { ".java" });
		File[] fs = new File[allFiles.length];
		int i = 0;
		for (String f : allFiles) {
			String path = f.replace(".java", "");
			fs[i++] = new File(path);
		}
		return fs;
	}

	/**
	 * delete orphaned java artifacts from the japidviews directory
	 * 
	 * @param packageRoot
	 * 
	 * @return
	 */
	static boolean rmOrphanJava(String packageRoot) {

		boolean hasRealOrphan = false;
		try {
			String pathname = getJapidviewsDir(packageRoot);
			File src = new File(pathname);
			if (!src.exists()) {
				log("Could not find required Japid root directory: " + pathname);
				return hasRealOrphan;
			}

			Set<File> oj = DirUtil.findOrphanJava(src, null);
			for (File j : oj) {
				String path = j.getPath();
				// JapidFlags.log("found: " + path);
				if (path.contains(DirUtil.JAVATAGS)) {
					JapidFlags
							.warn("using util classes in _javatags folder is deprecated and will not take effect in post-controller Japid mode. "
									+ "Please use Play app's standard app/utils folder and import the classes accordingly");
					// java tags, don't touch
				} else {
					hasRealOrphan = true;
					String realfile = pathname + File.separator + path;
					File file = new File(realfile);
					boolean r = file.delete();
					if (r)
						JapidFlags.debug("deleted orphan " + realfile);
					else
						JapidFlags.error("failed to delete: " + realfile);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return hasRealOrphan;
	}

	static List<File> reloadChanged() {
		return reloadChanged(defaultTemplateRoot);
	}

	static void log(String m) {
		JapidFlags.error("[JapidPlayRenderer]: " + m);
	}

	static void gen() {
		if (defaultTemplateRoot == null) {
			throw new RuntimeException("the template root directory must be set");
		} else {
			try {
				gen(defaultTemplateRoot);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// /**
	// * set to development mode
	// */
	// public static void setDevMode() {
	// devMode = true;
	// }

	// /**
	// * set to production mode
	// */
	// public static void setProdMode() {
	// devMode = false;
	// }
	//

	public static boolean isDevMode() {
		return getMode().isDev();
	}

	static String removeSemi(String imp) {
		imp = imp.trim();
		if (imp.endsWith(";")) {
			imp = imp.substring(0, imp.length() - 1);
		}
		return imp;
	}

	//
	// public <T extends JapidTemplateBaseWithoutPlay> String render(Class<T> c,
	// Object... args) {
	// int modifiers = c.getModifiers();
	// if (Modifier.isAbstract(modifiers)) {
	// throw new
	// RuntimeException("Cannot init the template class since it's an abstract class: "
	// + c.getName());
	// }
	// try {
	// Constructor<T> ctor = c.getConstructor(StringBuilder.class);
	// StringBuilder sb = new StringBuilder(8000);
	// T t = ctor.newInstance(sb);
	// RenderResult rr = RenderInvokerUtils.render(t, args);
	// return rr.getContent().toString();
	// } catch (NoSuchMethodException e) {
	// throw new
	// RuntimeException("Could not match the arguments with the template args.");
	// } catch (InstantiationException e) {
	// throw new
	// RuntimeException("Could not instantiate the template object. Abstract?");
	// } catch (Exception e) {
	// if (e instanceof RuntimeException)
	// throw (RuntimeException) e;
	// else
	// throw new RuntimeException("Could not invoke the template object: " + e);
	// }
	// }

	public static void addStaticImportLine(String imp) {
		importlines.add(" static " + removeSemi(imp));
	}

	/**
	 * 
	 * @param imp
	 *            the part after import, no ";"
	 */
	public static void addImportLine(String imp) {
		importlines.add(removeSemi(imp));
	}

	/**
	 * one of the ways to invoke a renderer
	 * 
	 * @param cls
	 * @param args
	 * @return
	 */
	public static String render(Class<? extends JapidTemplateBaseWithoutPlay> cls, Object... args) {
		RenderResult r = RenderInvokerUtils.invokeRender(cls, args);
		return r.getText();
	}

	/**
	 * The <em>optional</em> initialization step in using the JapidRender.
	 * 
	 * Users do not need to call this method to initialize this class. It takes
	 * "japidroot" as the default template root, which is relative to the
	 * application root.
	 * 
	 * @param opMode
	 *            the operational mode of Japid. When set to OpMode.prod, it's
	 *            assumed that all Java derivatives are already been generated
	 *            and used directly. When set to OpMode.dev, and using
	 *            none-static linking to using the renderer, file system changes
	 *            are detected for every rendering request given the refresh
	 *            interval is respected. New Java files are generated and
	 *            compiled and new classes are loaded to serve the request.
	 * @param templateRoot
	 *            the root directory to contain the "japidviews" directory tree.
	 *            It must be out of the "app" directory, or it will interfere
	 *            with Play's class loading.
	 * @param refreshInterval
	 *            the minimal time, in second, that must elapse before trying to
	 *            detect any changes in the file system.
	 */
	public static void init(Mode opMode, String templateRoot, int refreshInterval) {
		JapidFlags.info("JapidPlayRenderer initializing");
		mode = opMode;
		setTemplateRoot(templateRoot);
		setRefreshInterval(refreshInterval);
		initJapidClassLoader();
		compiler = new RendererCompiler(japidClasses, japidClassLoader);
		try {
			refreshClasses();
		} catch (Exception e) {
			JapidFlags
					.warn("There was an error in refreshing the japid classes. Will show the error in detail in processing a request: "
							+ e);
		}
		JapidFlags.info("JapidPlayRenderer inited");
	}

	public static void init() {
		init(null, defaultTemplateRoot, 2);
	}

	static {
		init();
	}

	/**
	 * a facet method to wrap implicit template binding. The default template is
	 * named as the class and method that immediately invoke this method. e.g.
	 * for an invocation scenario like this
	 * 
	 * <pre>
	 * package pack;
	 * 
	 * public class Foo {
	 * 	public String bar() {
	 * 		return JapidRender.render(p);
	 * 	}
	 * }
	 * </pre>
	 * 
	 * The template to use is "{templateRoot}/japidviews/pack/Foo/bar.html".
	 * 
	 * @param p
	 * @return
	 */
	public static String render(Object... args) {
		String templateName = findTemplate();
		RenderResult r = JapidController.getRenderResultWith(templateName, args);
		return r.getText();
	}

	/**
	 * render with the specified template.
	 * 
	 * @param templateName
	 *            The template must be rooted in the {templateRoot/}/japidviews
	 *            tree. The template name starts with or without "japidviews".
	 *            The naming pattern is the same as in
	 *            ClassLoader.getResource().
	 * @param args
	 * @return the result string
	 */
	public static String renderWith(String templateName, Object... args) {
		RenderResult r = JapidController.getRenderResultWith(templateName, args);
		return r.getText();
	}

	private static String findTemplate() {
		String japidRenderInvoker = StackTraceUtils.getJapidRenderInvoker();
		return japidRenderInvoker;
	}

	public static RenderResult handleException(Throwable e) throws RuntimeException {
		if (!presentErrorInHtml)
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException(e);

		// if (Play.mode == Mode.PROD)
		// throw new RuntimeException(e);
		//
		Class<? extends JapidTemplateBaseWithoutPlay> rendererClass = getErrorRendererClass();

		if (e instanceof JapidTemplateException) {
			RenderResult rr = RenderInvokerUtils.invokeRender(rendererClass, (JapidTemplateException) e);
			return (rr);
		}

		if (e instanceof RuntimeException && e.getCause() != null)
			e = e.getCause();

		if (e instanceof JapidCompilationException) {
			JapidCompilationException jce = (JapidCompilationException) e;
			JapidTemplateException te = JapidTemplateException.from(jce);
			RenderResult rr = RenderInvokerUtils.invokeRender(rendererClass, te);
			return (rr);
		}

		e.printStackTrace();

		// find the latest japidviews exception or the controller that caused
		// the exception
		StackTraceElement[] stackTrace = e.getStackTrace();
		for (StackTraceElement ele : stackTrace) {
			String className = ele.getClassName();
			if (className.startsWith("japidviews")) {
				int lineNumber = ele.getLineNumber();
				RendererClass applicationClass = japidClasses.get(className);
				if (applicationClass != null) {
					// let's get the line of problem
					int oriLineNumber = applicationClass.mapJavaLineToJapidScriptLine(lineNumber);
					if (oriLineNumber > 0) {
						if (rendererClass != null) {
							String path = applicationClass.getOriSourceCode();
							JapidTemplateException te = new JapidTemplateException("Japid Error", path + "("
									+ oriLineNumber + "): " + e.getClass().getName() + ": " + e.getMessage(),
									oriLineNumber, path, applicationClass.getOriSourceCode());
							RenderResult rr = RenderInvokerUtils.invokeRender(rendererClass, te);
							return (rr);
						}
					}
				}
			} else if (className.startsWith("controllers.")) {
				if (e instanceof RuntimeException)
					throw (RuntimeException) e;
				else
					throw new RuntimeException(e);
			}
		}

		JapidTemplateException te = new JapidTemplateException(e);
		RenderResult rr = RenderInvokerUtils.invokeRender(rendererClass, te);
		return rr;
		// if (e instanceof RuntimeException)
		// throw (RuntimeException) e;
		// else
		// throw new RuntimeException(e);
	}

	/**
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @return
	 */
	public static Class<? extends JapidTemplateBaseWithoutPlay> getErrorRendererClass() {
		// return devErrorClass;
		return japidviews.devError.class;
	}

	public static String renderPlayException(Exception e) {
		Exception exp = cn.bran.play.util.PlayExceptionUtils.mapJapidJavaCodeError(e);
		RenderResult rr = error500ForPlay.apply(exp);
		return rr.toString();
	}
}
