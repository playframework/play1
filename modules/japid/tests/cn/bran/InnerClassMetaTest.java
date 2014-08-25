package cn.bran;

import static org.junit.Assert.fail;

import org.junit.Test;

import cn.bran.japid.classmeta.InnerClassMeta;



public class InnerClassMetaTest {
	@Test
	public void testPrintout() {
		InnerClassMeta icm = new InnerClassMeta(
				"my.Display", 
				2,
				"String title, String hi", 
				"p (\"The real title is: \"); pln(title);"
				);
		System.out.println(icm.toString());
	}
	@Test
	public void testInvalidCallback() {
		try {
			InnerClassMeta icm = new InnerClassMeta(
					"my.Display", 
					2,
					"hi", 
					"p (\"The real title is: \"); pln(title);"
					);
			fail("shoud have caught it");
		} catch (RuntimeException e) {
			System.out.println(e);
		}
	}
}
