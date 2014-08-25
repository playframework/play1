package cn.bran.japid.compiler;

import static org.junit.Assert.*;

import org.junit.Test;

public class TemplateClassTest {
	private String validSrc = "package testdir.d1;\n" +
			"\n" +
			"public class A {\n" +
			" Boo boo = new Boo();\n" +
			"}\n" +
			"";

	private String invalidSrc = "package testdir.d1;\n" +
			"\n" +
			"public class A {\n" +
			" Boo boo;r\n" +
			"}\n" +
			"";

	@Test
	public void testValidClass() {
		TemplateClass tc = new TemplateClass(validSrc);
		assertNotNull(tc);
	}

	@Test (expected=RuntimeException.class)
	public void testInvalidClass() {
		TemplateClass tc = new TemplateClass(invalidSrc);
	}

	@Test
	public void testGetName() {
		TemplateClass tc = new TemplateClass(validSrc);
		assertEquals("A", tc.getName());
	}
}
