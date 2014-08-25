package cn.bran.japid.template;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.bran.japid.compiler.OpMode;
import cn.bran.japid.compiler.TranslateTemplateTask;
import cn.bran.japid.rendererloader.RendererClass;
import cn.bran.japid.rendererloader.RendererCompiler;
import cn.bran.japid.rendererloader.TemplateClassLoader;
import cn.bran.japid.util.DirUtil;
import cn.bran.japid.util.JapidFlags;
import cn.bran.japid.util.StackTraceUtils;

public class JapidRenderer {
	public static final String VERSION = "0.9.5.2"; // XXX need to match that in the build.xml

	static AmmendableScheduledExecutor saveJapidClassesService = new AmmendableScheduledExecutor();

	static boolean enableJITCachePersistence = true;

	public static JapidTemplateBaseWithoutPlay getRenderer(String name) {
		Class<? extends JapidTemplateBaseWithoutPlay> c = getClass(name);
		try {
			return c.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get a newly loaded class for the template renderer
	 * 
	 * @param name
	 * @return the renderer class wrapper
	 */
	public static RendererClass getFunctionalRendererClass(String name) {

		refreshClasses();

		RendererClass rc = classes.get(name);
		if (rc == null)
			throw new RuntimeException("renderer class not found: " + name);
		else {
			Class<? extends JapidTemplateBaseWithoutPlay> cls = rc.getClz();
			if (cls == null) {
				loadRendererClass(rc);
			}
		}
		return rc;
	}

	/**
	 * 
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param name
	 * @return the renderer class
	 */
	public static Class<? extends JapidTemplateBaseWithoutPlay> getClass(String name) {
		return getFunctionalRendererClass(name).getClz();
	}

	private static Class<? extends JapidTemplateBaseWithoutPlay> loadRendererClass(RendererClass rc) {
		// the template may refer to model classes etc available only from the
		// parent class loader
		ClassLoader cl = _parentClassLoader == null ? JapidRenderer.class.getClassLoader() : _parentClassLoader;
		// do I need to new instance of TemplateClassLoader for each invocation?
		// likely...
		TemplateClassLoader classReloader = new TemplateClassLoader(cl);
		try {
			Class<JapidTemplateBaseWithoutPlay> loadClass = (Class<JapidTemplateBaseWithoutPlay>) classReloader
					.loadClass(rc.getClassName());
			rc.setClz(loadClass);
			return loadClass;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	static boolean timeToRefresh() {
		if (inited) {
			if (!isDevMode())
				return false;
		}

		long now = System.currentTimeMillis();
		if (now - lastRefreshed > refreshInterval) {
			lastRefreshed = now;
			return true;
		} else
			return false;

	}

	static synchronized void refreshClasses() {
		if (!timeToRefresh())
			return;

		try {
			// find out all removed classes
			List<String> allHtml = DirUtil.getAllTemplateHtmlFiles(new File(templateRoot));
			Set<String> currentClassesOnDir = createNameSet(allHtml);
			Set<String> tmp = new HashSet<String>(currentClassesOnDir);

			Set<String> keySet = classes.keySet();
			tmp.removeAll(keySet); // added html

			removeRemoved(currentClassesOnDir, keySet);

			for (String c : tmp) {
				RendererClass rc = newRendererClass(c);
				classes.put(c, rc);
			}
			// now all the class set size is up to date

			// now update any Java source code
			List<File> gen = gen(templateRoot);

			// this would include both new and updated java
			Set<String> updatedClasses = new HashSet<String>();
			if (gen.size() > 0) {
				// int i = 0;
				for (File f : gen) {
					String className = getClassName(f);
					updatedClasses.add(className);
					RendererClass rendererClass = classes.get(className);
					if (rendererClass == null) {
						// this should not happen, since
						throw new RuntimeException("any new key should have been in the classes container: "
								+ className);
						// rendererClass = newRendererClass(className);
						// classes.put(className, rendererClass);
					}
					setSources(rendererClass, f);
					removeInnerClasses(className);
					cleanClassHolder(rendererClass);
				}
			}

			// find all render class without bytecode
			for (Iterator<String> i = classes.keySet().iterator(); i.hasNext();) {
				String k = i.next();
				RendererClass rc = classes.get(k);
				if (rc.getSourceCode() == null) {
					if (!rc.getClassName().contains("$")) {
						setSources(rc, getJavaSrcFile(k));
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
				compiler.compile(names);

				
				// shall I load them right away?
				// let's try
				ClassLoader cl = _parentClassLoader == null ? JapidRenderer.class.getClassLoader() : _parentClassLoader;
				TemplateClassLoader classReloader = new TemplateClassLoader(cl);
				for (RendererClass rc: classes.values()) {
					try {
						if (isDevMode())
							rc.setClz(null); // to enable JIT loading in dev mode
						else
							rc.setClz((Class<JapidTemplateBaseWithoutPlay>) classReloader.loadClass(rc.getClassName()));
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				}
				howlong("compile/load time for " + names.length + " classes", t);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void removeInnerClasses(String className) {
		for (Iterator<String> i = classes.keySet().iterator(); i.hasNext();) {
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
	public final static Map<String, RendererClass> classes = new ConcurrentHashMap<String, RendererClass>();
	public static TemplateClassLoader crlr;

	public static TemplateClassLoader getCrlr() {
		return crlr;
	}

	public static RendererCompiler compiler;
	public static String templateRoot = "plainjapid";
	public static final String JAPIDVIEWS = "japidviews";
	public static String sep = File.separator;
	public static String japidviews = templateRoot + sep + JAPIDVIEWS + sep;
	// such as java.utils.*
	public static List<String> importlines = new ArrayList<String>();
	public static int refreshInterval;
	public static long lastRefreshed;
	private static boolean inited;

	public static boolean isInited() {
		return inited;
	}

	private static OpMode opMode;
	private static boolean usePlay;

	public static OpMode getOpMode() {
		return opMode;
	}

	static void howlong(String string, long t) {
		JapidFlags.info(string + ":" + (System.currentTimeMillis() - t) + "ms");
	}

	/**
	 * @param rendererClass
	 */
	static void cleanClassHolder(RendererClass rendererClass) {
		rendererClass.setBytecode(null);
		rendererClass.setClz(null);
		rendererClass.setLastUpdated(0);
	}

	static Set<String> createNameSet(String[] allHtml) {
		// the names start with template root
		Set<String> names = new HashSet<String>();
		for (String f : allHtml) {
			names.add(getClassName(new File(f)));
		}
		return names;
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
		File f = getJavaSrcFile(k);
		return readSource(f);
	}

	private static File getJavaSrcFile(String k) {
		String pathname = templateRoot + sep + k;
		pathname = pathname.replace(".", sep);
		File f = new File(pathname + ".java");
		return f;
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

	static String readSource(File f) {
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

	public static String readFirstLine(File f) {
		try {
			FileInputStream fis = new FileInputStream(f);
			BufferedInputStream bis = new BufferedInputStream(fis, 160);
			BufferedReader br = new BufferedReader(new InputStreamReader(bis, "UTF-8"));
			String line = br.readLine();
			br.close();
			return line;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	static String getClassName(File f) {
		String path = f.getPath();
		String substring = path.substring(path.indexOf(JAPIDVIEWS));
		substring = substring.replace('/', '.').replace('\\', '.');
		if (substring.endsWith(".java")) {
			substring = substring.substring(0, substring.length() - 5);
		} else if (substring.endsWith(".html")) {
			substring = substring.substring(0, substring.length() - 5);
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

	static void setTemplateRoot(String root) {
		templateRoot = root;
		japidviews = templateRoot + sep + JAPIDVIEWS + sep;
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
				gen(templateRoot);
			} else if ("regen".equals(arg0)) {
				regen(templateRoot);
			} else if ("clean".equals(arg0)) {
				delAllGeneratedJava(getJapidviewsDir(templateRoot));
			} else if ("mkdir".equals(arg0)) {
				mkdir(templateRoot);
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
		String japidviewsDir = getJapidviewsDir(root);
		File layouts = new File(japidviewsDir + "_layouts");
		if (!layouts.exists()) {
			boolean mkdirs = layouts.mkdirs();
			assert mkdirs == true;
			log("created: " + layouts.getPath());
		}

		File tags = new File(japidviewsDir + "_tags");
		if (!tags.exists()) {
			boolean mkdirs = tags.mkdirs();
			assert mkdirs == true;
			log("created: " + tags.getPath());
		}

		File[] dirs = new File[] { layouts, tags };
		List<File> res = new ArrayList<File>();
		res.addAll(Arrays.asList(dirs));

		return res;

	}

	/**
	 * @param root
	 * @return
	 */
	private static String getJapidviewsDir(String root) {
		return root + sep + JAPIDVIEWS + sep;
	}

	static void regen() throws IOException {
		regen(templateRoot);
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
		// mkdir(packageRoot);
		// moved to reloadChanged
		List<File> changedFiles = reloadChanged(packageRoot);
		if (changedFiles.size() > 0) {
			// for (File f : changedFiles) {
			// // log("updated: " + f.getName().replace("html", "java"));
			// }
		} else {
			log("All java files are up to date.");
		}

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
				// log("found: " + path);
				hasRealOrphan = true;
				String realfile = pathname + File.separator + path;
				File file = new File(realfile);
				boolean r = file.delete();
				if (r)
					log("deleted orphan " + realfile);
				else
					log("failed to delete: " + realfile);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return hasRealOrphan;
	}

	static List<File> reloadChanged() {
		return reloadChanged(templateRoot);
	}

	static void log(String m) {
		JapidFlags.info("[JapidRender]: " + m);
	}

	static void gen() {
		if (templateRoot == null) {
			throw new RuntimeException("the template root directory must be set");
		} else {
			try {
				gen(templateRoot);
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
		return opMode == OpMode.dev;
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
	public String render(Class<? extends JapidTemplateBaseWithoutPlay> cls, Object... args) {
		return JapidPlainController.renderWith(cls, args);
	}

	public static void setSources(RendererClass rc, File f) {
		rc.setSourceCode(readSource(f));
		File srcFile = DirUtil.mapJavatoSrc(f);
		rc.setOriSourceCode(readSource(srcFile));
		rc.setSrcFile(srcFile);
	}

	/**
	 * The <em>required</em> initialization step in using the JapidRender.
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
	 * @param refreshInterval
	 *            the minimal time, in second, that must elapse before trying to
	 *            detect any changes in the file system.
	 * @param usePlay
	 *            to indicate if the generated template classes are to be used
	 *            with play. if true, Play's implicit objects are available in
	 *            the japid script.
	 */
	public static void init(OpMode opMode, String templateRoot, int refreshInterval, ClassLoader parentClassLoader,
			boolean usePlay) {
		JapidRenderer.opMode = opMode;
		setTemplateRoot(templateRoot);
		setRefreshInterval(refreshInterval);
		_parentClassLoader = parentClassLoader;
		if (_parentClassLoader == null) {
			_parentClassLoader = JapidRenderer.class.getClassLoader();
		}
		crlr = new TemplateClassLoader(parentClassLoader);
		compiler = new RendererCompiler(classes, crlr);
		JapidRenderer.usePlay = usePlay;
		refreshClasses();
		inited = true;
		System.out.println("[Japid] initialized version " + VERSION + " in " + getOpMode() + " mode");
	}

	/**
	 * see the other init method
	 * 
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @param opMode
	 * @param templateRoot
	 * @param refreshInterval
	 * @param parentClassLoader
	 */
	public static void init(OpMode opMode, String templateRoot, int refreshInterval, ClassLoader parentClassLoader) {
		init(opMode, templateRoot, refreshInterval, parentClassLoader, false);
	}

	private static ClassLoader _parentClassLoader = null;

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
		return JapidPlainController.renderJapidWith(templateName, args);
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
		return JapidPlainController.renderJapidWith(templateName, args);
	}

	private static String findTemplate() {
		String japidRenderInvoker = StackTraceUtils.getJapidRenderInvoker();
		return japidRenderInvoker;
	}

	public static String renderWith(Class<? extends JapidTemplateBaseWithoutPlay> cla, Object... args) {
		return JapidPlainController.renderWith(cla, args);
	}

}
