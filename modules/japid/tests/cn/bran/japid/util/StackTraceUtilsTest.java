package cn.bran.japid.util;

import static org.junit.Assert.*;

import org.junit.Test;

import cn.bran.play.JapidResult;

public class StackTraceUtilsTest {
	private static final int LOOPS = 10000;

	@Test public void testCallerInfo() {
		String r = foo();
		assertEquals(StackTraceUtilsTest.class.getName() + ".foo", r );
		r = bar2();
		String thisCaller = StackTraceUtilsTest.class.getName() + ".testCallerInfo";
		assertEquals(thisCaller, r );
		assertEquals(thisCaller, foo1() );
	}
	
	private String foo() {
		return bar();
	}

	private String bar() {
		return StackTraceUtils.getCaller();
	}

	private String bar2() {
		return StackTraceUtils.getCaller(2);
	}
	
	private String foo1() {
		return foo2();
	}

	private String foo2() {
		return StackTraceUtils.getCaller2();
	}
	
	/**
	 * it shows that each call is about 44us.
	 */
	@Test public void testPerformance() {
		char c =' ';
		
		// warm up
		for (int i = 0; i < 10; i++) {
			String m = StackTraceUtils.getCaller2();
			c = m.charAt(0);
		}

		long t = System.currentTimeMillis();
		int runs = 1000;
		for (int i = 0; i < runs; i++) {
			String m = StackTraceUtils.getCaller2();
			c = m.charAt(0);
		}
		System.out.println("runs " + runs + " took/ms: " + (System.currentTimeMillis() - t));
	}
	
	@Test
	public void testGetJapidRenderInvoker() {
		// how to test that?
	}

	@Test
	public void testGetInvokerOf() {
		String invokerOf = StackTraceUtils.getInvokerOf(StackTraceUtils.class.getName(), "getInvokerOf");
		assertEquals(this.getClass().getName() + "." + "testGetInvokerOf", invokerOf);
	}
	
	@Test
	public void testStackTracePerf() {
		String holder = "";
		int i = 0;
		long t = System.currentTimeMillis();
		while (i++ < LOOPS) {
			StackTraceElement[] ste = new Throwable().getStackTrace();
			holder = ste[2].getClassName();
			holder += ste[2].getMethodName();
		}
		System.out.println(LOOPS + " loops of native exceptino took:" + (System.currentTimeMillis() - t) + "ms :" + holder); ;

		t = System.currentTimeMillis();
		i = 0;
		while (i++ < LOOPS) {
			StackTraceElement[] ste = new JapidResult().getStackTrace();
			holder = ste == null? "null": "else";
//			holder += ste[2].getMethodName();
		}
		System.out.println(LOOPS + " loops of JapidResult took:" + (System.currentTimeMillis() - t) + "ms :" + holder); ;
	}
	
}
