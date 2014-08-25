package cn.bran.play;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import play.Play;
import play.data.validation.Validation;
import play.templates.JavaExtensions;
import play.vfs.VirtualFile;
import cn.bran.japid.compiler.JapidCompilationException;
import cn.bran.japid.compiler.JapidTemplateTransformer;
import cn.bran.japid.compiler.TranslateTemplateTask;
import cn.bran.japid.util.DirUtil;
import cn.bran.japid.util.JapidFlags;
import cn.bran.play.util.PlayDirUtil;

public class JapidCommands {
	private static final String APP = "app";

	/**
	 * TODO: perhaps I can parse the depended modules and apply the commands to them as well.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		try
		{
			String arg0 = args[0];

	//		String applicationPath = System.getProperty("user.dir");
			String applicationPath = ".";
			if (args.length > 1) {
				applicationPath = args[1];
			}
			Play.applicationPath = new File(applicationPath);
			String appPath = Play.applicationPath.getAbsolutePath() + File.separator;
			if ("gen".equals(arg0)) {
				gen(appPath + APP);
			} else if ("regen".equals(arg0)) {
				regen(appPath + APP);
			} else if ("clean".equals(arg0)) {
				delAllGeneratedJava(appPath + APP + File.separator + DirUtil.JAPIDVIEWS_ROOT);
			} else if ("mkdir".equals(arg0)) {
				PlayDirUtil.mkdir(appPath + APP);
			} else {
				JapidFlags.error("not known: " + arg0);
			}
		} catch (Throwable e) {
			JapidFlags.error(e.toString());
			System.exit(1);
		}
	}

	public static void regen() throws IOException {
		regen(APP);
	}

	public static void regen(String root) throws IOException {
		// TODO Auto-generated method stub
		String pathname = root + File.separatorChar + DirUtil.JAPIDVIEWS_ROOT;
		delAllGeneratedJava(pathname);
		gen(root);
	}

	public static void delAllGeneratedJava(String pathname) {
		String[] javas = DirUtil.getAllFileNames(new File(pathname), new String[] { "java" });

		for (String j : javas) {
			if (!j.contains(DirUtil.JAVATAGS)) {
				String filePath = pathname + File.separatorChar + j;
				JapidFlags.debug("removed: " + filePath);
				boolean delete = new File(filePath).delete();
				if (!delete)
					throw new RuntimeException("file was not deleted: " + j);
			}
		}
		// JapidFlags.log("removed: all none java tag java files in " +
		// JapidPlugin.JAPIDVIEWS_ROOT);
	}

	/**
	 * update the java files from the html files, for the changed only
	 * @throws IOException 
	 */
	public static void gen(String packageRoot) throws IOException {
		// moved to reloadChanged
		List<File> changedFiles = reloadChanged(packageRoot);
		if (changedFiles.size() > 0) {
			for (File f : changedFiles) {
				String relativePath = JapidTemplateTransformer.getRelativePath(f, Play.applicationPath);
				JapidFlags.info("updated: " + relativePath.replace("html", "java"));
			}
		} else {
			JapidFlags.info("No java files need to be updated.");
		}

		rmOrphanJava();
	}

	/**
	 * @param root
	 *            the package root "/"
	 * @return
	 */
	public static List<File> reloadChanged(String root) {
		try {
			DirUtil.mkdir(root);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		TranslateTemplateTask t = new TranslateTemplateTask();

		File rootDir = new File(root);
		t.setPackageRoot(rootDir);
		t.setInclude(new File(rootDir, DirUtil.JAPIDVIEWS_ROOT));
		t.clearImports();
		t.importStatic(JapidPlayAdapter.class);
		t.importStatic(Validation.class);
		t.importStatic(JavaExtensions.class);
		t.addAnnotation(NoEnhance.class);
		if (DirUtil.hasLayouts(root))
			t.addImport(DirUtil.JAPIDVIEWS_ROOT + "._layouts.*");
		if (DirUtil.hasJavaTags(root))
			t.addImport(DirUtil.JAPIDVIEWS_ROOT + "._javatags.*");
		if (DirUtil.hasTags(root))
			t.addImport(DirUtil.JAPIDVIEWS_ROOT + "._tags.*");
		t.addImport("models.*");
		t.addImport("controllers.*");
		t.addImport(play.mvc.Scope.class.getName() + ".*");
		t.addImport(play.i18n.Messages.class);
		t.addImport(play.i18n.Lang.class);
		t.addImport(play.mvc.Http.class.getName() + ".*");
		t.addImport(Validation.class.getName());
		t.addImport(play.data.validation.Error.class.getName());
//		t.addImport("static  japidviews._javatags.JapidWebUtil.*");
		List<String> javatags = DirUtil.scanJavaTags(root);
		for (String f : javatags) {
			t.addImport("static " + f + ".*");
		}
		try {
			t.execute();
		} catch (JapidCompilationException e) {
//			 remove the .class file from previous successful compilation if any
//			String templateName = e.getTemplateName();
//			String javaSrc = DirUtil.mapSrcToJava(templateName);
//			// remove the java file
//			String javaSrcPath = APP + File.separator + javaSrc;
//			if (new File(javaSrcPath).delete()){
//				System.out.println("[Japid] deleted: " + javaSrcPath);
//			}
//			String className = javaSrc.substring(0, javaSrc.length() - 5).replace('/', '.').replace('\\', '.');
//			// remove the class file
//			Play.classes.remove(className);
			throw e;
		}
		List<File> changedFiles = t.getChangedFiles();
		return changedFiles;
	}

	/**
	 * delete orphaned java artifacts from the japidviews directory of the current app and all the depended modules
	 * 
	 * @return
	 */
	public static boolean rmOrphanJava() {
		boolean hasOrphan = false;
		String appAbs = Play.applicationPath.getAbsolutePath() + File.separator + APP;
		hasOrphan = removeOrphanedJavaFrom(appAbs);
		
		Collection<VirtualFile> modules = Play.modules.values();
		for (VirtualFile module: modules) {
			try {
				VirtualFile root = module.child(APP);
				VirtualFile japidViewDir = root.child(DirUtil.JAPIDVIEWS_ROOT);
				File japidFile = japidViewDir.getRealFile();
				if (japidFile.exists()) {
					String absoluteRootPath = root.getRealFile().getAbsolutePath();
					if( removeOrphanedJavaFrom(absoluteRootPath))
						hasOrphan = true;
				}
			}
			catch(Throwable t) {
				
			}
			
		}
		return hasOrphan;
	}

	private static boolean removeOrphanedJavaFrom(String root) {
		boolean hasRealOrphan = false;
		try {
			String pathname = root + File.separator + DirUtil.JAPIDVIEWS_ROOT;
			File src = new File(pathname);
			if (!src.exists()) {
				JapidFlags.info("Could not find required Japid package structure: " + pathname);
				JapidFlags.info("Please use \"play japid:mkdir\" command to create the Japid view structure.");
				return hasRealOrphan;
			}

			Set<File> oj = DirUtil.findOrphanJava(src, null);
			for (File j : oj) {
				String path = j.getPath();
				// JapidFlags.log("found: " + path);
				if (path.contains(DirUtil.JAVATAGS)) {

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

	/**
	 * check the current app and dependencies for changed templates
	 * @return
	 */
	public static List<File> reloadChanged() {
		List<File> reloadChanged = reloadChanged(new File(Play.applicationPath, APP).getAbsolutePath());
		Collection<VirtualFile> modules = Play.modules.values();
		for (VirtualFile module: modules) {
			try {
				VirtualFile root = module.child(APP);
				VirtualFile japidViewDir = root.child(DirUtil.JAPIDVIEWS_ROOT);
				File japidFile = japidViewDir.getRealFile();
				if (japidFile.exists()) {
					String absoluteRootPath = root.getRealFile().getAbsolutePath();
					reloadChanged.addAll(reloadChanged(absoluteRootPath));
				}
			}
			catch(Throwable t) {
				
			}
			
		}
		return reloadChanged;
	}
	
}
