package cn.bran.japid.template;

import static org.junit.Assert.*;

import org.junit.Test;

import cn.bran.japid.compiler.OpMode;
import cn.bran.japid.template.FooController.ModelUser;

public class JapidPlainControllerTest {
	public static void main() throws InterruptedException {
		
		int i = 0;
		while (i++ < 3) {
			try {
				new JapidPlainControllerTest(). testRun();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Thread.sleep(3000);
		}		
	}

	/**
	 * @param p
	 */
	@Test
	public void testRun() {
		JapidRenderer.init(OpMode.dev, "plainjapid", 1, null);
		String p = "worldy ";
		long t = System.currentTimeMillis();
		String a1 = new FooController().a1(p);
		System.out.println(a1);
		System.out.println("==== took: " + (System.currentTimeMillis()  - t ));

		t = System.currentTimeMillis();
//				a1 = new FooController().foo(p);
		a1 = new FooController().bar(p);
		System.out.println(a1);
		
		long x = System.currentTimeMillis() - t;
		System.out.println("--------> took time(ms): " + x);
		
		a1 = new FooController().tee(new ModelUser("me and you"));
		System.out.println(a1);
	}
}
