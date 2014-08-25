package cn.bran.misc;

import org.junit.Before;
import org.junit.Test;

/**
 * was thinking warp all ${} in try catch to catch NPE and convert to "" in case of it. Users don't need to 
 * concerned with null value in multi-level properties access, such as a.b.c. 
 * 
 * I need to find out the performance of massive use of try catch
 * 
 * for a million times run, the baseline is about 6ms, while the try/catch block added 1-4 extra millis. 
 * So the conclusion is try catch will only incur about <1ms slowdown in a typical page rendering.
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 *
 */
public class TryCatchPerf {
	A a;
	static final int count = 1000000;
	
	@Before
	public void init() {
		a = new A();
		B b = new B();
		C c = new C();
		b.c = c;
		a.b = b;
	}
	
	
	@Test
	public void baseline() {
		long t0 = System.nanoTime();
		long cl = 0;
		for (int c = 0; c < count; c++) {
			cl += a.b.c.p.length();
		}
		System.out.println("baseline time cost/ms: " + (System.nanoTime() - t0)/1000.0/1000.0);
		System.out.println(cl);
	}
	
	@Test
	public void trycatch() {
		long t0 = System.nanoTime();
		long cl = 0;
		for (int c = 0; c < count; c++) {
			try {
				cl += a.b.c.p.length();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("try/catch time cost/ms: " + (System.nanoTime() - t0)/1000.0/1000.0);
		System.out.println(cl);
	}
	
	static class A{
		B b;
	}
	
	static class B {
		C c;
	}
	
	static class C {
		String p = "hello";
	}
}
