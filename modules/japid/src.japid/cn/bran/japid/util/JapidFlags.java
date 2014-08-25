package cn.bran.japid.util;

import cn.bran.japid.MyTuple2;
import cn.bran.japid.template.JapidTemplateBaseWithoutPlay;

public class JapidFlags {
	static enum LogLevel {
		debug(0), info(1), warn(2), error(3);
		LogLevel(int i) {
			this.level = i;
		}

		int level;

		/**
		 * @author Bing Ran (bing.ran@gmail.com)
		 * @param debug2
		 * @return
		 */
		public boolean noLowerThan(LogLevel debug2) {
			return level >= debug2.level;
		}

	}

	static LogLevel logLevel = LogLevel.warn;

	public static boolean verbose = true;

	private static StringBuffer buffer = new StringBuffer();

	private static int internBufferSize;

	/**
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @param string
	 */
	private static void log(String string) {
		if (verbose)
			out(string);
	}

	private static void out(String string) {
		if (!string.startsWith("[Japid"))
			string = "[Japid]" + " " + string.trim();
		System.out.println(string);
	}

	/**
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param string
	 */
	public static void debug(String string) {
		if (LogLevel.debug.noLowerThan(logLevel)) {
			out("[debug] " + string);
		}
	}

	public static void warn(String string) {
		if (LogLevel.warn.noLowerThan(logLevel)) {
			out("[warn] " + string);
		}
	}

	public static void setLogLevel(LogLevel l) {
		out("set log level to " + l);
		logLevel = l;
	}

	/**
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param string
	 */
	public static void info(String string) {
		if (LogLevel.info.noLowerThan(logLevel)) {
			out("[info] " + string);
		}
	}

	public static void error(String string) {
		if (LogLevel.error.noLowerThan(logLevel)) {
			out("[error] " + string);
		}
	}

	public static void setLogLevelDebug() {
		setLogLevel(LogLevel.debug);
	}

	public static void setLogLevelInfo() {
		setLogLevel(LogLevel.info);
	}

	public static void setLogLevelWarn() {
		setLogLevel(LogLevel.warn);
	}

	public static void setLogLevelError() {
		setLogLevel(LogLevel.error);
	}

	/**
	 * for internal use to buffer debugging information. 
	 * 
	 * It should NOT be used by externally by end users. 
	 * 
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param string
	 */
	public static void _log(String string) {
		// TODO Auto-generated method stub
		buffer.append(string);
		internBufferSize = buffer.length();
	}

	public static void _flush() {
		if (internBufferSize > 0) {
			String string = buffer.toString();
			System.out.println(string);
			buffer = new StringBuffer();
		}
	}

	public static void logTimeLogs(JapidTemplateBaseWithoutPlay t) {
		if (t.isStopwatch()) {
			String src = t.sourceTemplate;
			out("The time logs for: " + src);
			if (t.timeLogs.size() > 0) {
				for (MyTuple2<String, Long> tup : t.timeLogs) {
					_log(tup._1() + ": " + tup._2() + "μs\n"); 
				}
			}
		}
		_flush();
	}

}
