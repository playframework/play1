package cn.bran.japid.compiler;

import static org.junit.Assert.*;

import org.junit.Test;

public class JapidTemplateTransformerTest {

	@Test
	public void testLooksLikeLayout() {
		String src = " #{doLayout /} ";
		assertTrue(JapidTemplateTransformer.looksLikeLayout(src));

		src = "<p> #{ doLayout /} </p>";
		assertTrue(JapidTemplateTransformer.looksLikeLayout(src));

		src = "<p> `doLayout`</p> ";
		assertTrue(JapidTemplateTransformer.looksLikeLayout(src));

		src = "<p> @doLayout@</p> ";
		assertTrue(JapidTemplateTransformer.looksLikeLayout(src));
		
		src = "<p> `doLayout\n";
		assertTrue(JapidTemplateTransformer.looksLikeLayout(src));

		src = "<p> @doLayout\n";
		assertTrue(JapidTemplateTransformer.looksLikeLayout(src));

		src = "<p> `doLayout\r\n";
		assertTrue(JapidTemplateTransformer.looksLikeLayout(src));

		src = "`doLayoutter ";
		assertFalse(JapidTemplateTransformer.looksLikeLayout(src));

		src = "<p> #{get \"something\"/}</p>";
		assertTrue(JapidTemplateTransformer.looksLikeLayout(src));

		src = "<p> #{ get \"something\"/}</p>";
		assertTrue(JapidTemplateTransformer.looksLikeLayout(src));

		src = "<p> `get something  ";
		assertTrue(JapidTemplateTransformer.looksLikeLayout(src));

		src = "<p> @get something  ";
		assertTrue(JapidTemplateTransformer.looksLikeLayout(src));
		
		src = "`get something";
		assertTrue(JapidTemplateTransformer.looksLikeLayout(src));

		src = "@get something";
		assertTrue(JapidTemplateTransformer.looksLikeLayout(src));

		src = "`getter (something)";
		assertFalse(JapidTemplateTransformer.looksLikeLayout(src));
	}

}
