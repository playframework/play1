/**
 * Copyright 2010 Bing Ran<bing_ran@hotmail.com> 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at http://www.apache.org/licenses/LICENSE-2.0.
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package cn.bran.japid.compiler;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.regex.Pattern;

import cn.bran.japid.classmeta.AbstractTemplateClassMetaData;
import cn.bran.japid.classmeta.MimeTypeEnum;
import cn.bran.japid.template.JapidTemplate;
import cn.bran.japid.util.DirUtil;

/**
 * compile html based template to java files
 * 
 * The facade to all the compiler suite and configurations.
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * 
 */
public class JapidTemplateTransformer {

	private static final String HTML = ".html";
	private String sourceFolder;
	private String targetFolder;
	private boolean usePlay = true;

	// private MessageProvider messageProvider;
	// private UrlMapper urlMapper;

	/**
	 * @param sourceFolder
	 *            the folder containing the template source tree. It's the same
	 *            concept as in eclipse. The root package for Java artifacts are
	 *            the direct children of this
	 * @param targetFolder
	 *            the "source folder", in Eclipse term, that the generated Java
	 *            artifacts will be placed. If it is null, the Java files will
	 *            be placed in the sourceFolder.
	 */
	public JapidTemplateTransformer(String sourceFolder, String targetFolder) {
		super();

		this.sourceFolder = sourceFolder;
		// this.messageProvider = messageProvider;
		// this.urlMapper = urlMapper;
		this.targetFolder = targetFolder;
		//		
		// BranTemplateBase.messageProvider = messageProvider;
		// BranTemplateBase.urlMapper = urlMapper;
	}

	/**
	 * 
	 * @param importLine
	 *            add an import to all the files generated. For examples:
	 *            "my.package.*", "my.package.MyClass"
	 */
	public static void addImportLine(String importLine) {
		AbstractTemplateClassMetaData.addImportLineGlobal(importLine);
	}

	/**
	 * effectively as in Java: "import static my.Tools.*;" if Tools.class is the
	 * parameter.
	 * 
	 * @param class1
	 */
	public static void addImportStatic(Class<?> class1) {
		AbstractTemplateClassMetaData.addImportStatic(class1);
	}

	static Pattern doLayoutTag = Pattern.compile(".*\\#\\{\\s*doLayout\\s*.*");
	static Pattern doLayoutDirective = Pattern.compile(".*[`@]doLayout");
	static Pattern doLayoutDirective2 = Pattern.compile(".*[`@]doLayout\\s?[`@].*");
	static Pattern getTag= Pattern.compile(".*\\#\\{\\s*get\\s*.*");
	static Pattern getDirective = Pattern.compile(".*[`@]get\\s+\\w+\\s*");
	
	public static boolean looksLikeLayout(String src) {
		String[] split = src.split("[\r\n]");
		for (String line : split) {
			line = line.trim();
			if (doLayoutTag.matcher(line).matches()
					|| doLayoutDirective.matcher(line).matches()
					|| doLayoutDirective2.matcher(line).matches()
					|| getTag.matcher(line).matches()
					|| getDirective.matcher(line).matches()) {
				return true;
			}
		}
		return false;
	}
	/**
	 * 
	 * @param fileName
	 *            the relative path of the template file from the source folder,
	 *            e.g., "my/path/mytemplate.html", which maps to
	 *            my.path.mytemplate.java
	 * @return
	 * @throws Exception
	 */
	public File generate(String fileName) throws Exception {
		String realSrcFile = sourceFolder == null ? fileName : sourceFolder + "/" + fileName;
		String src = DirUtil.readFileAsString(realSrcFile);
		JapidTemplate temp = new JapidTemplate(fileName, src);
		JapidAbstractCompiler c = selectCompiler(src);
		c.setUseWithPlay(usePlay);
		c.compile(temp);
		String jsrc = temp.javaSource;

		String fileNameRoot = DirUtil.mapSrcToJava(fileName);

		String target = targetFolder == null ? sourceFolder : targetFolder;
		String realTargetFile = target == null ? fileNameRoot : target + "/" + fileNameRoot;
		
		File f = DirUtil.writeToFile(jsrc, realTargetFile);
		return f;

	}

	public static JapidAbstractCompiler selectCompiler(String src) {
		JapidAbstractCompiler c = null;
		// TODO: more robust way of determine layout file or view file
		if (looksLikeLayout(src)) {
			c = new JapidLayoutCompiler();
		} else {
			// regular template and tag are the same thing
			c = new JapidTemplateCompiler();
		}
		return c;
	}

	/**
	 * 
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @param scriptSrc the Japid script source code
	 * @param srcFileName the full path to the script file. Used to parse package and class name
	 * @return the generated Java code
	 * 
	 */
	public static String generateInMemory(String scriptSrc,  String srcFileName, boolean usePlay) {
		JapidTemplate temp = new JapidTemplate(srcFileName, scriptSrc);
		return compileJapid(scriptSrc, usePlay, temp);
	}

	private static String compileJapid(String scriptSrc, boolean usePlay, JapidTemplate temp) {
		JapidAbstractCompiler c = null;
		if (looksLikeLayout(scriptSrc)) {
			c = new JapidLayoutCompiler();
		} else {
			// regular template and tag are the same thing
			c = new JapidTemplateCompiler();
		}
		c.setUseWithPlay(usePlay);
		c.compile(temp);
		String jsrc = temp.javaSource;
		return jsrc;
	}


	/**
	 * a utility method. Should be somewhere else.
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

	/**
	 * transform a source template to Java
	 * 
	 * @param file
	 *            the source file that must be a descendant of the source folder.
	 * @return
	 * @throws Exception
	 */
	public File generate(File file) throws Exception {
		String rela = DirUtil.getRelativePath(file, new File("."));
		return generate(rela);
	}

	/**
	 * add class level annotation for whatever purpose
	 * 
	 * @param anno
	 */
	public static void addAnnotation(Class<? extends Annotation> anno) {
		AbstractTemplateClassMetaData.addAnnotation(anno);
		// typeAnnotations.add(anno);
	}

	public void usePlay(boolean b) {
		this.usePlay = b;
	}

	// List<Class<? extends Annotation>> typeAnnotations = new ArrayList<Class<?
	// extends Annotation>>();
}
