package cn.bran.japid.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.bran.japid.classmeta.AbstractTemplateClassMetaData;
import cn.bran.japid.template.JapidRenderer;


public class DirUtil {
	public static final String OF = ", ";
	public static final String LINE_MARKER = "// line ";
	static final String DOT_SUB = "_d_";

	private static final String[] ALL_EXTS = new String[] { ".java", ".html", ".js", ".txt", ".css", ".json", ".xml" };
	private static final String[] TEMPLATE_EXTS = new String[]{".html", ".js", ".txt", ".css", ".xml", ".json"};
	
	public static Set<File> findOrphanJava(File src, File target) {
		if (target == null)
			target = src;
		String[] allSrc = getAllFileNames(src, ALL_EXTS);
		Set<String> javas = new HashSet<String>();
		Set<String> srcFiles = new HashSet<String>();

		for (String s : allSrc) {
			if (s.endsWith(".java")) {
				javas.add(s);
			} else /*if (s.endsWith(".html"))*/ {
				srcFiles.add(mapSrcToJava(s));
			}
		}

		javas.removeAll(srcFiles);
		Set<File> re = new HashSet<File>();
		for (String j : javas) {
			re.add(new File(j));
		}
		return re;
	}

	
	/**
	 * 
	 */
	public static String[] getAllFileNames(File dir, String[] exts) {
		List<String> files = new ArrayList<String>();
		getAllFileNames("", dir, files, exts);
		String[] ret = new String[files.size()];
		return files.toArray(ret);
	}
	
	public static List<String> getAllTemplateHtmlFiles(File dir) {
		List<String> files = new ArrayList<String>();
		getAllFileNames("", dir, files, new String[] {".html"});
		keepJapidFiles(files);
		return files;
	}


	private static void keepJapidFiles(List<String> files) {
		for (Iterator<String> it = files.iterator(); it.hasNext();) {
			if (!it.next().startsWith(JAPIDVIEWS_ROOT)) {
				it.remove();
			}
		}
	}
	
	public static List<String> getAllTemplateFiles(File dir) {
		List<String> files = new ArrayList<String>();
		getAllFileNames("", dir, files, TEMPLATE_EXTS);
		keepJapidFiles(files);
		return files;
	}
	
	public static List<String> getAllTemplateFilesJavaFiles(File dir) {
		List<String> files = new ArrayList<String>();
		getAllFileNames("", dir, files, ALL_EXTS);
		keepJapidFiles(files);
		return files;
	}
	
	

	/**
	 * collect all files with one of the extensions from the directory. 
	 * @param dir
	 * @param exts
	 * @param fs
	 * @return the files match. Note the files path starts with the source dir.
	 */
	public static Set<File> getAllFiles(File dir, String[] exts, Set<File> fs) {
		Set<File> scanFiles = scanFiles(dir, exts, fs);
		return scanFiles;
	}

	/**
	 * @param dir
	 * @param exts
	 * @param fs
	 * @return
	 */
	private static Set<File> scanFiles(File dir, String[] exts, Set<File> fs) {
		File[] flist = dir.listFiles();
		for (File f : flist) {
			if (f.isDirectory())
				getAllFiles(f, exts, fs);
			else {
				if (match(f, exts))
					fs.add(f);
			}
		}
		return fs;
	}
	
	private static void getAllFileNames(final String leadingPath, final File dir, final List<String> files, final String[] exts) {
		if (!dir.exists())
			return;
//			throw new RuntimeException("directory exists? " +  dir.getPath());
		
		File[] flist = dir.listFiles();
		for (File f : flist) {
			if (f.isDirectory())
				getAllFileNames(leadingPath + f.getName() + File.separatorChar, f, files, exts);
			else {
				if (match(f, exts))
					files.add(leadingPath + f.getName());
			}
		}
	}

	static boolean match(File f, String[] exts) {
		for (String ext : exts) {
			if (f.getName().endsWith(ext))
				if (fileNameIsValidClassName(f))
					return true;
		}
		return false;
	}

//	private static boolean firstTimeDetectingChanges = true;
	
	static private Set<File> versionCheckedDirs  = new HashSet<File>();
	
	public static List<File> findChangedSrcFiles(File srcDir) {
//		if (target == null)
//			target = src;
//		String srcPath = src.getPath();
		Set<File> allSrc  = new HashSet<File>();
		allSrc = getAllFiles(srcDir, ALL_EXTS, allSrc);
		
		// <name, last modified time>
		Map<String, Long> javaFiles = new HashMap<String, Long>();
		Map<String, Long> scriptFiles = new HashMap<String, Long>();
		
		long now = System.currentTimeMillis();
		boolean hasFutureTimestamp = false;
		for (File f : allSrc) {
			String path = f.getPath();
			long modi = f.lastModified();
//			System.out.println("file: " + path + ":" + modi);
			if (path.endsWith(".java")) {
				if (modi > now) {
					// for some reason(e.g., copies from other file system), the last modified time is in the future
					// let's reset it.
					hasFutureTimestamp = true;
					f.setLastModified(now - 1);
				}
				javaFiles.put(path, modi);
			} else  {
				// validate file name to filter out dubious files such as temporary files
				if (fileNameIsValidClassName(f)) {
					if (modi > now) {
						// for some reason(e.g., copies from other file system), the last modified time is in the future
						// let's reset it.
						hasFutureTimestamp = true;
						f.setLastModified(now);
					}
					scriptFiles.put(path, modi);
				}
			}
		}

		if (hasFutureTimestamp) {
			JapidFlags.warn("Some of the Japid files have a timestamp in the future. It could have been caused by out-of-synch system time.");
		}
		
		List<File> changedScripts = new ArrayList<File>();
		
		boolean japidVersionChecked = versionCheckedDirs.contains(srcDir);
		
		for (String script : scriptFiles.keySet()) {
			String javaFileName = mapSrcToJava(script);
			File javaFile = new File(javaFileName);
			Long javaTimestamp = javaFiles.get(javaFileName);
			
			if (javaTimestamp == null) {
				// how can this happen?
//				javaFile.delete();
				changedScripts.add(new File(script));
			}
			else {
				Long srcStamp = scriptFiles.get(script);
				if (srcStamp.compareTo(javaTimestamp) > 0) {
//					javaFile.delete();
					changedScripts.add(new File(script));
				}
				else {
//					if (firstTimeDetectingChanges) {
					if (!japidVersionChecked) {
						// check the japid version that the java code was generated by
						if (javaFile.exists()) {
							String firstLine = JapidRenderer.readFirstLine(javaFile);
							if (firstLine.startsWith(AbstractTemplateClassMetaData.VERSION_HEADER)) {
								firstLine = firstLine.substring(AbstractTemplateClassMetaData.VERSION_HEADER.length()).trim();
								if (!JapidRenderer.VERSION.equals(firstLine)) {
									JapidFlags.debug("japid version mismatch. to refresh " + javaFileName);
//									javaFile.delete();
									changedScripts.add(new File(script));
								}
							}
							else {
								JapidFlags.debug("japid version mismatch. to refresh " + javaFileName);
//								javaFile.delete();
								changedScripts.add(new File(script));
							}
						}
					}
				}
			}
		}
		versionCheckedDirs.add(srcDir);
//		firstTimeDetectingChanges = false;
		return changedScripts;
	}

	/**
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @param f
	 * @return
	 */
	private static boolean fileNameIsValidClassName(File f) {
		String fname = f.getName();
		if (fname.startsWith("."))
			return false;
		fname = fname.substring(0, fname.lastIndexOf(".")).replace('.', '_');
		return isClassname(fname);
	}

	/**
	 * map template source file name to the generated java file name
	 * @param k
	 * @return
	 */
	public static String mapSrcToJava(String k) {
		if (k.endsWith(".java"))
				return k;
		if (k.endsWith(".txt")) {
			return getRoot(k) + "_txt" + ".java";
		}
		else if (k.endsWith(".xml")) {
			return getRoot(k) + "_xml" + ".java";
		}
		else if (k.endsWith(".json")) {
			return getRoot(k) + "_json" + ".java";
		}
		else if (k.endsWith(".css")) {
			return getRoot(k) + "_css" + ".java";
		}
		else if (k.endsWith(".js")) {
			return getRoot(k) + "_js" + ".java";
		}
		else { // including html
			return getRoot(k) + ".java";
		}
	}
	
	/**
	 * 
	 * map java source file name to the template file name
	 * @param k
	 * @return
	 */
	public static String mapJavaToSrc(String k) {
		return rawConvert(k);//.replaceAll(DOT_SUB, ".");
	}

	private static String rawConvert(String k) {
		if (k.endsWith(".java"))
			k = k.substring(0, k.lastIndexOf(".java"));
		
		if (k.endsWith("_txt")) {
			return k.substring(0, k.lastIndexOf("_txt")) + ".txt";
		}
		else if (k.endsWith("_xml")) {
			return k.substring(0, k.lastIndexOf("_xml")) + ".xml";
		}
		else if (k.endsWith("_json")) {
			return k.substring(0, k.lastIndexOf("_json")) + ".json";
		}
		else if (k.endsWith("_css")) {
			return k.substring(0, k.lastIndexOf("_css")) + ".css";
		}
		else if (k.endsWith("_js")) {
			return k.substring(0, k.lastIndexOf("_js")) + ".js";
		}
		else { // including html
			return  k + ".html";
		}
	}
 
	private static String getRoot(String k) {
		for (String ext : TEMPLATE_EXTS) {
			if (k.endsWith(ext)) {
				String sub = k.substring(0, k.lastIndexOf(ext));
//				String first = "";
//				if (sub.startsWith(".")) { // keep the first dot, which means current dir
//					first = ".";
//					sub = sub.substring(1);
//				}
//				return first + sub.replaceAll("\\.", DOT_SUB);
				return sub;
			}
		}
		return k;
	}

	public static void writeStringToFile(File file, String content) throws IOException {
		Writer fw = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
		BufferedWriter bw = new BufferedWriter(fw);
		bw.append(content);
		bw.close();
	}

	public static void touch(File newer) throws IOException {
		writeStringToFile(newer, "");
	}

	public static boolean containsTemplateFiles(String root, String dirName) {
		String sep = File.separator;
		String japidViews = root + sep + JAPIDVIEWS_ROOT + sep;
		String dir = japidViews + dirName;
		return containsTemplatesInDir(dir);
	}

	public static boolean containsTemplatesInDir(String dirName) {
		File dir = new File(dirName);
		
		if (dir.exists()) {
			String[] temps = getAllFileNames(dir, TEMPLATE_EXTS);
			if (temps.length > 0) 
				return true;
			else
				return false;
		}
		else
			return false;
	}

	public static boolean hasTags(String root) {
		String dirName = DirUtil.TAGSDIR;
		return containsTemplateFiles(root, dirName);
	}

	public static boolean hasJavaTags(String root) {
		String dirName = DirUtil.JAVATAGS;
		return containsTemplateFiles(root, dirName);
	}

	public static boolean hasLayouts(String root) {
		String dirName = DirUtil.LAYOUTDIR;
		return containsTemplateFiles(root, dirName);
	}
	
	 public static boolean isClassname( String classname ) {
	      if (classname == null || classname.length() ==0) return false;

          CharacterIterator iter = new StringCharacterIterator(classname);
          // Check first character (there should at least be one character for each part) ...
          char c = iter.first();
          if (c == CharacterIterator.DONE) return false;
          if (!Character.isJavaIdentifierStart(c) && !Character.isIdentifierIgnorable(c)) return false;
          c = iter.next();
          // Check the remaining characters, if there are any ...
          while (c != CharacterIterator.DONE) {
              if (!Character.isJavaIdentifierPart(c) && !Character.isIdentifierIgnorable(c)) return false;
              c = iter.next();
          }
	      return true;
	  }

	public static final String JAVATAGS = "_javatags";
	public static final String LAYOUTDIR = "_layouts";
	public static final String TAGSDIR = "_tags";
	public static final String JAPIDVIEWS_ROOT = "japidviews";
	public static List<String> scanJavaTags(String root) {
		String sep = File.separator;
		String japidViews = root + sep + JAPIDVIEWS_ROOT + sep;
		File javatags = new File(japidViews + JAVATAGS);
		if (!javatags.exists()) {
			boolean mkdirs = javatags.mkdirs();
			assert mkdirs == true;
			JapidFlags.info("created: " + japidViews + JAVATAGS);
		}
	
		File[] javafiles = javatags.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(".java"))
					return true;
				return false;
			}
		});
		
		List<String> files = new ArrayList<String>();
		for (File f : javafiles) {
			String fname = f.getName();
			files.add(JAPIDVIEWS_ROOT + "." + JAVATAGS + "." + fname.substring(0, fname.lastIndexOf(".java")));
		}
		return files;
	}

	/**
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @param f the java file
	 * @return the original template file
	 */
	public static File mapJavatoSrc(File f) {
		File parent = f.getParentFile();
		String fname = mapJavaToSrc(f.getName());
		return new File(parent, fname);
	}
	
	/**
	 * 
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @param sourceCode
	 * @param lineNum 1-based line number in Java file
	 * @return 1-based line number in the original source template
	 */
	public static int mapJavaLineToSrcLine(String sourceCode, int lineNum) {
		String[] codeLines = sourceCode.split("\n");
		String line = codeLines[lineNum - 1];
	
		int lineMarker = line.lastIndexOf(LINE_MARKER);
		if (lineMarker < 1) {
			return -1;
		}
		String linenum = line.substring(lineMarker + LINE_MARKER.length()).trim();
		int indexOf = linenum.indexOf(OF);
		if (indexOf > 0) {
			linenum = linenum.substring(0, indexOf);
		}
		return Integer.parseInt(linenum);
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
		public static List<File> mkdir(String root) throws IOException {
			String sep = File.separator;
			String japidViews = root + sep + JAPIDVIEWS_ROOT + sep;
			File javatags = new File(japidViews + JAVATAGS);
			if (!javatags.exists()) {
				boolean mkdirs = javatags.mkdirs();
				assert mkdirs;
				JapidFlags.info("created: " + japidViews + JAVATAGS);
			}
	
	//		File webutil = new File(javatags, "JapidWebUtil.java");
	//		if (!webutil.exists()) {
	//			DirUtil.writeStringToFile(webutil, JapidWebUtil);
	//			JapidFlags.log("created JapidWebUtil.java.");
	//		}
			// add the place-holder for utility class for use in templates
	
			File layouts = new File(japidViews + LAYOUTDIR);
			if (!layouts.exists()) {
				boolean mkdirs = layouts.mkdirs();
				assert mkdirs;
				JapidFlags.info("created: " + japidViews + LAYOUTDIR);
			}
	
			File tags = new File(japidViews + TAGSDIR);
			if (!tags.exists()) {
				boolean mkdirs = tags.mkdirs();
				assert mkdirs;
				JapidFlags.info("created: " + japidViews + TAGSDIR);
			}
			
			// email notifiers
			File notifiers = new File(japidViews + "_notifiers");
			if (!notifiers.exists()) {
				boolean mkdirs = notifiers.mkdirs();
				assert mkdirs;
				JapidFlags.info("created: " + japidViews + "_notifiers");
			}
			
			
			File[] dirs = new File[] { javatags, layouts, tags };
			List<File> res = new ArrayList<File>();
			res.addAll(Arrays.asList(dirs));
	
			// create dirs for controllers
	
	//		JapidFlags.log("JapidCommands: check default template packages for controllers.");
			try {
				String controllerPath = root + sep + "controllers";
				File controllerPathFile = new File(controllerPath);
				if (controllerPathFile.exists()) {
					String[] controllers = DirUtil.getAllJavaFilesInDir(controllerPathFile);
					for (String f : controllers) {
						String cp = japidViews + f;
						File ff = new File(cp);
						if (!ff.exists()) {
							boolean mkdirs = ff.mkdirs();
							assert mkdirs == true;
							res.add(ff);
							JapidFlags.info("created: " + cp);
						}
					}
				}
			} catch (Exception e) {
				JapidFlags.error(e.toString());
			}
	
	//		JapidFlags.log("JapidCommands:  check default template packages for email notifiers.");
			try {
				String notifiersDir = root + sep + "notifiers";
				File notifiersDirFile = new File(notifiersDir);
				if (!notifiersDirFile.exists()) {
					if (notifiersDirFile.mkdir()) {
						JapidFlags.info("created the email notifiers directory. ");
					}
					else {
						JapidFlags.info("email notifiers directory did not exist and could not be created for unknow reason. ");
					}
				}
				
				String[] controllers = DirUtil.getAllJavaFilesInDir(notifiersDirFile);
				for (String f : controllers) {
					// note: we keep the notifiers dir to differentiate those from the controller
					// however this means we cannot have a controller with package like "controllers.notifiers"
					// so we now use "_notifiers"
					String cp = japidViews + "_notifiers" + sep + f;
					File ff = new File(cp);
					if (!ff.exists()) {
						boolean mkdirs = ff.mkdirs();
						assert mkdirs == true;
						res.add(ff);
						JapidFlags.info("created: " + cp);
					}
				}
			} catch (Exception e) {
				JapidFlags.error(e.toString());
			}
			return res;
		}


	/**
	 * get all the java files in a dir with the "java" removed
	 * 
	 * @return
	 */
	public static String[] getAllJavaFilesInDir(File root) {
		// from source files only
		String[] allFiles = getAllFileNames(root, new String[] { ".java" });
		for (int i=0; i< allFiles.length; i++) {
			allFiles[i] = allFiles[i].replace(".java", "");
		}
		return allFiles;
	}

	/**
	 * a utility method.
	 * 
	 * @param srcDir
	 * @param cf
	 * @throws IOException
	 */
	public static String getRelativePath(File child, File parent) throws IOException {
		String curPath = parent.getCanonicalPath();
		String childPath = child.getCanonicalPath();
		assert (childPath.startsWith(curPath));
		String srcRelative = childPath.substring(curPath.length());
		if (srcRelative.startsWith(File.separator)) {
			srcRelative = srcRelative.substring(File.separator.length());
		}
		return srcRelative;
	}

	
	// this method is entirely safe ???
	static public String readFileAsString(String filePath) throws Exception {
		byte[] buffer = new byte[(int) new File(filePath).length()];
		BufferedInputStream f = new BufferedInputStream(new FileInputStream(filePath));
		f.read(buffer);
		f.close();
		return new String(buffer, "UTF-8");
	}


	public static File writeToFile(String jsrc, String realTargetFile) throws FileNotFoundException, IOException,
			UnsupportedEncodingException {
		File f = new File(realTargetFile);
		if (!f.exists()) {
			String parent = f.getParent();
			new File(parent).mkdirs();
		}
		BufferedOutputStream bf = new BufferedOutputStream(new FileOutputStream(f));
		bf.write(jsrc.getBytes("UTF-8"));
		bf.close();
		return f;
	}

}
