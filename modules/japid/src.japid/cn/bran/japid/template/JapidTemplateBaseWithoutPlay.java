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
package cn.bran.japid.template;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import cn.bran.japid.MyTuple2;
import cn.bran.japid.tags.Each;
import cn.bran.japid.tags.Each.BreakLoop;
import cn.bran.japid.tags.Each.ContinueLoop;
import cn.bran.japid.util.StringUtils;

import cn.bran.japid.classmeta.MimeTypeEnum;
import cn.bran.japid.compiler.NamedArg;
import cn.bran.japid.compiler.NamedArgRuntime;
import cn.bran.japid.util.HTMLUtils;
import cn.bran.japid.util.JapidFlags;
import cn.bran.japid.util.WebUtils;

/**
 * a java based template suing StringBuilder as the content buffer, no play
 * dependency.
 * 
 * @author bran
 * 
 */
public abstract class JapidTemplateBaseWithoutPlay implements Serializable {
	public String sourceTemplate = "";
	private StringBuilder out;
	private Map<String, String> headers;// = new TreeMap<String, String>();


	// directive for tracing templates navigation
	private Boolean traceFile = null;

	private String contentType = "";

	private Boolean stopwatch = null;
	long startTime = 0; // nano-second when starting rendering
	protected long renderingTime = -1; // in microsecond

	// the template that calls this as a tag
	protected JapidTemplateBaseWithoutPlay caller;

	// <marker, time consumption>
	public List<MyTuple2<String, Long>> timeLogs;
	
	private void init() {
		if (headers == null) {
			 headers = new TreeMap<String, String>();
//			 headers = new HashMap<String, String>();
			 headers.put("Content-Type", "text/html; charset=utf-8");
		}
		if (timeLogs == null) {
			timeLogs = new LinkedList<MyTuple2<String, Long>>();
		}
		if (out == null)
			out = new StringBuilder(4000);
	}

	public void setOut(StringBuilder out) {
		this.out = out;
	}

	protected StringBuilder getOut() {
		return out;
	}

	// public JapidTemplateBase() {
	//
	// };

	protected void putHeader(String k, String v) {
		headers.put(k, v);
	}

	protected Map<String, String> getHeaders() {
		return this.headers;
	}

	public JapidTemplateBaseWithoutPlay(StringBuilder out2) {
		this.out = out2;
		init();
	}

	public JapidTemplateBaseWithoutPlay(JapidTemplateBaseWithoutPlay caller) {
		if (caller != null) {
			out = caller.getOut();
		}
		this.caller = caller;
		this.timeLogs = caller.timeLogs;
		this.headers = caller.getHeaders();
		this.stopwatch = caller.stopwatch;
		init();
	}

	// don't use it since it will lead to new instance of stringencoder
	// Charset UTF8 = Charset.forName("UTF-8");

	final protected void p(String s) {
		if (s != null && !s.isEmpty())
			out.append(s);

//		writeString(s);
	}

	final protected void pln(String s) {
		if (s != null && !s.isEmpty())
			out.append(s);

//		writeString(s);
		out.append('\n');
	}

	/**
	 * @param s
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	final private void writeString(String s) {
		// ByteBuffer bb = StringUtils.encodeUTF8(s);
		// out.write(bb.array(), 0, bb.position());
		// ok my code is slower in large trunk of data
		if (s != null && !s.isEmpty())
			out.append(s);
	}

	// final protected void pln(byte[] ba) {
	// try {
	// out.write(ba);
	// out.write('\n');
	// } catch (IOException e) {
	// throw new RuntimeException(e);
	// }
	// }

	final protected void p(Object s) {
		if (s != null) {
			writeString(s.toString());
			// out.append(s);
		}
	}

	final protected void pln(Object s) {
		if (s != null)
			writeString(s.toString());
		pln();
	}

	final protected void pln() {
		out.append('\n');
	}

	/**
	 * The template pattern to implement the template/layout relationship.
	 * Clients call a template's render(), which store params in fields and
	 * calls in super class's layout, which does the whole page layout and calls
	 * back child's doLayout to get the child content.
	 */
	protected void layout() {
		doLayout();
	}

	protected abstract void doLayout();

	static protected byte[] getBytes(String src) {
		if (src == null || src.length() == 0)
			return new byte[] {};

		try {
			return src.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return this.out.toString();
	}

	/**
	 * reflect this object for a method of the name
	 * 
	 * @param methodName
	 * @return
	 */
	protected String get(String methodName, String defaultVal) {
		try {
			Method method = this.getClass().getMethod(methodName, (Class[]) null);
			String invoke = (String) method.invoke(this, (Object[]) null);
			return invoke;
		} catch (Exception e) {
			return defaultVal;
		}
	}

	/**
	 * reflect this object for a method of the name
	 * 
	 * @param methodName
	 * @return
	 */
	protected String get(String methodName) {
		try {
			Method method = this.getClass().getMethod(methodName, (Class[]) null);
			String invoke = (String) method.invoke(this, (Object[]) null);
			return invoke;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected boolean asBoolean(Object o) {
		return WebUtils.asBoolean(o);
	}

	/**
	 * escape the string representation of the object to make it HTML safe.
	 * 
	 * @param o
	 * @return
	 */
	public static String escape(Object o) {
		if (o == null)
			return null;
		return HTMLUtils.htmlEscape(o.toString());
	}

	/**
	 * @param currentClass
	 */
	public static Method getRenderMethod(Class<? extends JapidTemplateBaseWithoutPlay> currentClass) {
		java.lang.reflect.Method[] methods = currentClass.getDeclaredMethods();

		Method r = null;
		for (java.lang.reflect.Method m : methods) {
			if (m.getName().equals("render")) {
				Class<?>[] parameterTypes = m.getParameterTypes();
				int paramLength = parameterTypes.length;
				if (paramLength == 1) {
					Class<?> t = parameterTypes[0];
					if (t != NamedArgRuntime[].class) {
						if (r == null)
							r = m;
					}
				} else {
					boolean hasNamedArg = false;
					for (Class<?> c : parameterTypes) {
						if (c == NamedArgRuntime.class || c == NamedArgRuntime[].class) {
							hasNamedArg = true;
							break;
						}
					}
					if (!hasNamedArg) {
						// a candidate. choose the one with longer param list
						if (r == null)
							r = m;
						else if (paramLength > r.getParameterTypes().length)
							r = m;
					}
				}
			}
		}
		if (r != null)
			return r;
		else
			throw new RuntimeException("no render method found for the template: " + currentClass.getCanonicalName());
	}

	/*
	 * based on https://github.com/branaway/Japid/issues/12 This static mapping
	 * will be later user in method renderModel to construct an proper Object[]
	 * array which is needed to invoke the method render(Object... args) over
	 * reflection.
	 */

	public java.lang.reflect.Method renderMethodInstance;
	public boolean hasDoBody = false;

	protected void setHasDoBody() {
		hasDoBody = true;
	}

	protected void setRenderMethod(Method renderMethod) {
		// System.out.println("-> setrender name: " + renderMethod);
		renderMethodInstance = renderMethod;
	}

	public String[] argNamesInstance = null;

	protected void setArgNames(String[] argNames) {
		// System.out.println("-> set args names: " + argNames);
		this.argNamesInstance = argNames;
	}

	public String[] argTypesInstance = null;

	protected void setArgTypes(String[] argTypes) {
		// System.out.println("-> set args names: " + argNames);
		this.argTypesInstance = argTypes;
	}

	public Object[] argDefaultsInstance = null;
	private MimeTypeEnum mimeType;
	private Boolean traceFileExit = null;
	public static boolean globalTraceFile = false;
	public static Boolean globalTraceFileHtml = null;
	public static Boolean globalTraceFileJson = null;

	protected void setArgDefaults(Object[] argDefaults) {
		// System.out.println("-> set args names: " + argNames);
		this.argDefaultsInstance = argDefaults;
	}

	// public cn.bran.japid.template.RenderResult
	// renderModel(cn.bran.japid.template.JapidModelMap model) {
	// // a static utils method of JapidModelMap to build up an Object[] array.
	// Nulls are used where the args are omitted.
	// Object[] args = model.buildArgs(argNamesInstance);
	// try {
	// return (cn.bran.japid.template.RenderResult )
	// renderMethodInstance.invoke(this, args);
	// } catch (IllegalArgumentException e) {
	// throw new RuntimeException(e);
	// } catch (IllegalAccessException e) {
	// throw new RuntimeException(e);
	// } catch (InvocationTargetException e) {
	// Throwable t = e.getTargetException();
	// throw new RuntimeException(t);
	// }
	// }
	//
	protected static NamedArgRuntime named(String name, Object val) {
		return new NamedArgRuntime(name, val);
	}

	public cn.bran.japid.template.RenderResult render(NamedArgRuntime... named) {
		// a static utils method of JapidModelMap to build up an Object[] array.
		// Nulls are used where the args are omitted.
		Object[] args = null;
		if (hasDoBody) // called without the callback block
			args = buildArgs(named, null);
		else
			args = buildArgs(named);
		return runRenderer(args);
	}

	/**
	 * @param args
	 * @return
	 */
	protected cn.bran.japid.template.RenderResult runRenderer(Object[] args) {
		try {
			return (cn.bran.japid.template.RenderResult) renderMethodInstance.invoke(this, args);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			throw new RuntimeException(t);
		}
	}

	/**
	 * build
	 * 
	 * @param argNames
	 * @param namedArgs
	 * @return
	 */
	public Object[] buildArgs(NamedArgRuntime[] namedArgs) {
		Map<String, Object> map = new HashMap<String, Object>();
		for (NamedArgRuntime na : namedArgs) {
			map.put(na.name, na.val);
		}

		Object[] ret = new Object[argNamesInstance.length];

		for (int i = 0; i < argNamesInstance.length; i++) {
			String name = argNamesInstance[i];
			if (map.containsKey(name)) {
				ret[i] = map.remove(name);
			} else {
				// any default set?
				Object defa = this.argDefaultsInstance[i];
				if (defa != null)
					ret[i] = defa;
				else {
					// set default value for primitives and Strings, or null for
					// complex object
					String type = argTypesInstance[i];
					Object defaultVal = getDefaultValForType(type);
					ret[i] = defaultVal;
				}
			}
		}
		if (map.size() > 0) {
			Set<String> keys = map.keySet();
			String sep = ", ";
			String ks = "[" + StringUtils.join(keys, sep) + "]";
			String vs = "[" + StringUtils.join(argNamesInstance, sep) + "]";
			throw new RuntimeException("One or more argument names are not valid: " + ks
					+ ". Valid argument names are: " + vs);
		}
		return ret;
	}

	protected Object[] buildArgs(NamedArgRuntime[] named, Object body) {
		Object[] obsNoBody = buildArgs(named);
		int len = obsNoBody.length;
		Object[] ret = new Object[len + 1];
		System.arraycopy(obsNoBody, 0, ret, 0, len);
		ret[len] = body;
		return ret;
	}

	private static Object getDefaultValForType(String type) {
		if (type.equals("String"))
			return "";
		else if (/* type.equals("Boolean") || */type.equals("boolean"))
			return false;
		else if (type.equals("char") /* || type.equals("Character") */)
			return (char) 0;
		else if (type.equals("byte") /* || type.equals("Byte") */)
			return (byte) 0;
		else if (type.equals("short") /* || type.equals("Short") */)
			return (short) 0;
		else if (type.equals("int") /* || type.equals("Integer") */)
			return 0;
		else if (type.equals("float") /* || type.equals("Float") */)
			return 0f;
		else if (type.equals("long") /* || type.equals("Long") */)
			return 0L;
		else if (type.equals("double") /* || type.equals("Double") */)
			return 0d;

		return null;
	}

	protected void handleException(RuntimeException e) {
		throw e;
	}

	protected void setSourceTemplate(String st) {
		this.sourceTemplate = st;
	}

	/**
	 * templates call this method to insert the current template name in a
	 * mine-type sensitive comment. It does not respect the `tracefile
	 * directive. It's useful to mark template files that generate xml/xhtml
	 * that requires doctype tag in the first line of the output. It's a
	 * convenient substitute of the `tracefile directive in such cases.
	 * 
	 * @author Bing Ran (bing.ran@hotmail.com)
	 */
	protected void traceFile() {
		this.traceFileExit = true;
		p(makeBeginBorder(this.sourceTemplate));
	}

	/**
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @return
	 */
	protected String makeBeginBorder(String viewSource) {
		if (StringUtils.isEmpty(contentType))
			return null;

		String formatter = getContentCommentFormatter(contentType);
		if (formatter == null)
			return "";

		return String.format(formatter, "enter: \"" + viewSource + "\"");

	}

	/**
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @return
	 */
	protected String makeEndBorder(String viewSource) {
		if (StringUtils.isEmpty(contentType))
			return null;

		String formatter = getContentCommentFormatter(contentType);
		if (formatter == null)
			return "";

		String content = "exit: \"" + viewSource + "\"";
		if (shouldRecordTime()) {
			// add time consumption to the endline for debugging purpose
			content += ". Duration/μs: " + renderingTime;
		}

		return String.format(formatter, content);

	}

	/**
	 * determine if the current template should mark the entrance and the exit
	 * in the output
	 * 
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @return
	 */
	private boolean shouldTraceFile() {
		if (traceFile != null)
			return traceFile;
		else if (this.mimeType == MimeTypeEnum.xml || this.mimeType == MimeTypeEnum.html)
			if (globalTraceFileHtml != null)
				return globalTraceFileHtml;
			else
				return globalTraceFile;
		else if (this.mimeType == MimeTypeEnum.js || this.mimeType == MimeTypeEnum.json)
			if (globalTraceFileJson != null)
				return globalTraceFileJson;
			else
				return globalTraceFile;

		return false;

	}

	protected void beginDoLayout(String viewSource) {
		if (shouldTraceFile())
			p(makeBeginBorder(viewSource));
		if (isStopwatch()) {
			startTime = System.nanoTime();
		} else if (shouldRecordTime()) {
			startTime = System.nanoTime();
		}
	}

	/**
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @return
	 */
	private boolean shouldRecordTime() {
//		if (caller != null && caller.shouldRecordTime())
//			return true;
//		else
			return isStopwatch();
	}

	protected void endDoLayout(String viewSource) {
		if (shouldRecordTime()) {
			calcDuration();
			logTime(sourceTemplate, renderingTime);
		}

		if (shouldTraceFile())
			p(makeEndBorder(viewSource));
		else if (traceFileExit != null && traceFileExit)
			p(makeEndBorder(viewSource));

	}

	private void calcDuration() {
		long duration = System.nanoTime() - startTime;
		renderingTime = duration / 1000;

//		JapidFlags._log("Time consumed to render \"" + sourceTemplate + "\": " + renderingTime + " μs");
	}

	public static String getContentCommentFormatter(String contentTypeString) {
		if (contentTypeString.contains("xml") || contentTypeString.contains("html"))
			return "<!-- %s -->";

		if (contentTypeString.contains("json") || contentTypeString.contains("javascript")
				|| contentTypeString.contains("css"))
			return "/* %s */";
		return null;
	}

	/**
	 * @return the contentType
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * @param contentType
	 *            the contentType to set
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
		if (contentType.contains("xml"))
			this.mimeType = MimeTypeEnum.xml;
		else if (contentType.contains("html"))
			this.mimeType = MimeTypeEnum.html;
		else if (contentType.contains("javascript"))
			this.mimeType = MimeTypeEnum.js;
		else if (contentType.contains("json"))
			this.mimeType = MimeTypeEnum.json;
		else if (contentType.contains("css"))
			this.mimeType = MimeTypeEnum.css;
	}

	/**
	 * @return the traceFile
	 */
	public Boolean getTraceFile() {
		return traceFile;
	}

	/**
	 * @param traceFile
	 *            the traceFile to set
	 */
	public void setTraceFile(Boolean traceFile) {
		this.traceFile = traceFile;
	}

	/**
	 * @deprecated the Each tag is deprecated in favor of using native loop
	 */
	protected void breakLoop() {
		throw new BreakLoop();
	}

	/**
	 * @deprecated the Each tag is deprecated in favor of using native loop
	 */
	protected void continueLoop() {
		throw new ContinueLoop();
	}

	/**
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param strings
	 * @return
	 */
	protected static int getCollectionSize(Object col) {

		if (col instanceof Collection) {
			return ((Collection) col).size();
		}

		if (col.getClass().isArray()) {
			return Array.getLength(col);
		}

		if (col instanceof Iterable || col instanceof Iterator) {
			return -1;
		}

		return -1;
	}

	public boolean isStopwatch() {
		return stopwatch != null ? stopwatch : false;
	}

	public void setStopwatchOn() {
		this.stopwatch = true;
	}
// XXX reconsider this later. consider layout with param case carefully.
//	protected void startRendering() {
//		try {
//			layout();
//		} catch (RuntimeException __e) {
//			handleException(__e);
//		}
//	}

	protected cn.bran.japid.template.RenderResult getRenderResult() {
		return new cn.bran.japid.template.RenderResult(getHeaders(), getOut(), renderingTime);
	}

	/**
	 * For debugging purpose. Can be called by `
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param marker
	 */
	protected void logDuration(String marker) {
		if (shouldRecordTime()) {
			long endtime = System.nanoTime();
			long duration = endtime - startTime;
			long t = duration / 1000;

			logTime(marker, t);
			// JapidFlags._log("Time consumed up to \"" + marker + "\" in \"" +
			// sourceTemplate + "\": " + t + " μs");
		}
	}

	/**
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param marker
	 * @param t
	 */
	private void logTime(String marker, long t) {
//		if (caller != null) {
//			caller.logTime(marker, t);
//		} else {
			timeLogs.add(new MyTuple2(marker, t));
//		}
	}
}
