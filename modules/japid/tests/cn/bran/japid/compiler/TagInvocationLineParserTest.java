package cn.bran.japid.compiler;

import static org.junit.Assert.*;

import org.junit.Test;

public class TagInvocationLineParserTest {
	TagInvocationLineParser p = new TagInvocationLineParser();

	@Test
	public void testParseSimplest() {
		String src = "tag";
		Tag t = p.parse(src);
		assertEquals("tag", t.tagName);
		assertEquals("", t.args);
	}

	@Test
	public void testParseSimple() {
		String src = "tag a, b";
		Tag t = p.parse(src);
		assertEquals("tag", t.tagName);
		assertEquals("a,b", t.args);
	}

	@Test
	public void testClosureParamError() {
		String src = "tag a, b | c";
		Tag t = p.parse(src);
	}
	
	@Test
	public void testEachClosureParamError() {
		String src = "Each a, b | c";
		try {
			Tag t = p.parse(src);
			fail("should have thrown an exception");
		} catch (RuntimeException e) {
			System.out.println(e);
		}
		
		src = "Each a | int c";
		Tag t = p.parse(src);
	}

	@Test
	public void testOrString() {
		String src = "tag a, b | \"string\"";
		Tag t = p.parse(src);
	}

	@Test
	public void testParseSimple2() {
		String src = "tag(a, b)";
		Tag t = p.parse(src);
		assertEquals("tag", t.tagName);
		assertEquals("a,b", t.args);
	}

	@Test
	public void testParseSimple3() {
		String src = "tag	 (a, b)";
		Tag t = p.parse(src);
		assertEquals("tag", t.tagName);
		assertEquals("a,b", t.args);
	}
	
	@Test
	public void testParseWithClosure() {
		String src = "tag(a, b) | String c";
		Tag t = p.parse(src);
		assertEquals("tag", t.tagName);
		assertEquals("a, b", t.args);
		assertEquals("String c", t.callbackArgs);
		assertTrue(t.hasBody);
	}

	@Test
	public void testNoArgWithClosure() {
		String src = "tag | String c";
		Tag t = p.parse(src);
		assertEquals("tag", t.tagName);
		assertEquals("", t.args);
		assertEquals("String c", t.callbackArgs);
		assertTrue(t.hasBody);
	}

	@Test
	public void testCallbackWithoutArgs() {
		String src = "tag a |";
		Tag t = p.parse(src);
		assertEquals("tag", t.tagName);
		assertEquals("a", t.args);
		assertEquals("", t.callbackArgs);
		assertTrue(t.hasBody);
	}

	@Test
	public void testNoArgWithClosure2() {
		String src = "tag()| String c";
		Tag t = p.parse(src);
		assertEquals("tag", t.tagName);
		assertEquals("", t.args);
		assertEquals("String c", t.callbackArgs);
		assertTrue(t.hasBody);
	}

	@Test
	public void testWithNoClosure2() {
		String src = "tag(a, b)";
		Tag t = p.parse(src);
		assertEquals("tag", t.tagName);
		assertEquals("a,b", t.args);
		assertNull(t.callbackArgs);
	}

	@Test
	public void testStringLiteral() {
		String src = "get \"title\"";
		Tag t = p.parse(src);
		assertEquals("get", t.tagName);
		assertEquals("\"title\"", t.args);
		assertNull(t.callbackArgs);
	}

	@Test
	public void testTagWithPath() {
		String src = "my.tag a, b";
		Tag t = p.parse(src);
		assertEquals("my.tag", t.tagName);
		assertEquals("a,b", t.args);
		assertNull(t.callbackArgs);
	}

	@Test
	public void testTagWithPathSep() {
		String src = "my/tag a, b";
		Tag t = p.parse(src);
		assertEquals("my.tag", t.tagName);
		assertEquals("a,b", t.args);
		assertNull(t.callbackArgs);
	}

	@Test
	public void testTagWithRelativeDot() {
		String src = ".my.tag a, b";
		Tag t = p.parse(src);
		assertEquals(".my.tag", t.tagName);
		assertEquals("a,b", t.args);
		assertNull(t.callbackArgs);
	}

	@Test
	public void testTagWithRelativeSep() {
		String src = "./my/tag a, b";
		Tag t = p.parse(src);
		assertEquals(".my.tag", t.tagName);
		assertEquals("a,b", t.args);
		assertNull(t.callbackArgs);
	}

	@Test
	public void testNamedArgs() {
		String src = "tag name=\"a\", age=b + 1";
		Tag t = p.parse(src);
		assertEquals("tag", t.tagName);
		assertEquals("named(\"name\", \"a\"), named(\"age\", b + 1)", t.args);
	}

	@Test
	public void testVerticalBars() {
		String src = "tag name=\"|\", age=b + 1";
		Tag t = p.parse(src);
		assertEquals("tag", t.tagName);
		assertEquals("named(\"name\", \"|\"), named(\"age\", b + 1)", t.args);
	}

	@Test
	public void testClosureParams() {
		String src = "tag |String a b";
		try {
			Tag t = p.parse(src);
			fail("should have thrown an exception");
		} catch (Exception e) {
			System.out.println(e);
		}
	}

}
