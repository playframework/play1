package cn.bran.japid.compiler;

import static org.junit.Assert.*;

import java.util.regex.Matcher;

import org.junit.Test;

public class JapidTemplateCompilerTest {
	@Test
	public void testSetPattern() {
		Matcher matcher = JapidTemplateCompiler.setPattern.matcher("title \"my great title\"");
		assertTrue(matcher.matches());
		assertEquals("title", matcher.group(1));
		assertEquals("\"my great title\"", matcher.group(2));
		
	}
	@Test
	public void testSetPattern2() {
		Matcher matcher = JapidTemplateCompiler.SET_ARG_PATTERN_ONELINER.matcher("title=a");
		assertTrue(matcher.matches());
		matcher = JapidTemplateCompiler.SET_ARG_PATTERN_ONELINER.matcher("title = \"home: \"");
		assertTrue(matcher.matches());
		matcher = JapidTemplateCompiler.SET_ARG_PATTERN_ONELINER.matcher("title: \"home: \"");
		assertTrue(matcher.matches());
	}

	@Test
	public void testSetPattern3() {
		Matcher matcher = JapidTemplateCompiler.SET_ARG_PATTERN_ONELINER_COLON.matcher("title : a = b");
		assertTrue(matcher.matches());
		matcher = JapidTemplateCompiler.SET_ARG_PATTERN_ONELINER_COLON.matcher("title = \"home: \"");
		assertFalse(matcher.matches());
	}
}
