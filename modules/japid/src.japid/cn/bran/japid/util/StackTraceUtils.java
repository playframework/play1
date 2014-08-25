package cn.bran.japid.util;

import cn.bran.japid.template.JapidRenderer;

//import static org.junit.Assert.*;
//
//import org.junit.Test;

public class StackTraceUtils {

	public static String getCaller() {
		int depth = 3;
		return getCaller(depth);
	}

	public static String getCaller2() {
		int depth = 4;
		return getCaller(depth);
	}

	public static String getCaller3() {
		int depth = 5;
		return getCaller(depth);
	}

	/**
	 * @param depth
	 * @return
	 */
	public static String getCaller(int depth) {
		final StackTraceElement[] ste = new Throwable().getStackTrace();
		StackTraceElement st = ste[depth];
		return st.getClassName() + "." + st.getMethodName();
	}

	public static String getCurrentMethodFullName() {
		return getCaller(2);
	}

	public static String getJapidRenderInvoker() {
		return getInvokerOf(JapidRenderer.class.getName(), "render");
	}

	/**
	 * @return
	 */
	public static String getInvokerOf(String targetClassName, String methodName) {
		final StackTraceElement[] ste = new Throwable().getStackTrace();
		for (int i = 0; i < ste.length; i++) {
			StackTraceElement st = ste[i];
			String className = st.getClassName();
			String method = st.getMethodName();
			if (className.equals(targetClassName) && method.equals(methodName)) {
				// the next one is what I want
				st = ste[i + 1];
				className = st.getClassName();
				method = st.getMethodName();
				return className + "." + method;
			}
		}
		return null;
	}

	static class QuickThrowable extends Throwable {
		@Override
		public synchronized Throwable fillInStackTrace() {
			return this;
		}
	}
}
