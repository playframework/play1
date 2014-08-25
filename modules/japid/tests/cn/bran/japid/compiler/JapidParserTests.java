package cn.bran.japid.compiler;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.junit.Test;

import cn.bran.japid.compiler.JapidParser;
import cn.bran.japid.compiler.JapidParser.Token;

public class JapidParserTests {

	/**
	 * make sure the parser can parse value expression without {}
	 */
	@Test
	public void testSimpleExpression() {

		final String D = "~";
		final String L = "{";
		final String R = "}";

		String source = "~ ~ ~ur	~u ~_ ~_index3\r\n ~\"hello\".length.hi(foo(var+ 's'))etc... ~a=='a'";
		// String source = "hello ~user.name.toUpperCase()! ~user";
		String[] se = new String[] {
				"~ ~ ",
				"ur",
				"	",
				"u",
				" ",
				"_",
				" ",
				"_index3",
				"\r\n ",
				"\"hello\".length.hi(foo(var+ 's'))",
				"etc... ",
				"a=='a'",
				};

		int i = 0;
		String src =
				se[i++] +
						D + se[i++] +
						se[i++] +
						D + se[i++] +
						se[i++] +
						D + se[i++] +
						se[i++] +
						D + se[i++] +
						se[i++] +
						D + se[i++] +
						se[i++] +
						D + se[i++];
		assertEquals(source, src);
		List<String> tokens = new ArrayList<String>();
		JapidParser p = new JapidParser(src);
		loop: for (;;) {
			JapidParser.Token state = p.nextToken();
			switch (state) {
			case EOF:
				break loop;
			default:
				String tokenstring = p.getToken();
				tokens.add(tokenstring);
				System.out.println(state.name() + ": [" + tokenstring + "]");
			}
		}

//		assertEquals(se.length, tokens.size());
		for (i = 0; i < se.length; i++) {
			assertEquals(se[i], tokens.get(i));
		}
	}

	@Test
	public void testDollarJQuery() {

		final String D = "$";
		final String L = "{";
		final String R = "}";

		String source = "$(doc) $('a') $'a' $doc('b')";
		List<String> tokens = new ArrayList<String>();
		JapidParser p = new JapidParser(source);
		loop: for (;;) {
			JapidParser.Token state = p.nextToken();
			switch (state) {
			case EOF:
				break loop;
			default:
				String tokenstring = p.getToken();
				tokens.add(tokenstring);
				System.out.println(state.name() + ": [" + tokenstring + "]");
			}
		}

		int i = 0;
//		assertEquals(4, tokens.size());
		assertEquals("$(doc) $('a') ", tokens.get(i++));
		assertEquals("'a'", tokens.get(i++));
		assertEquals(" ", tokens.get(i++));
		assertEquals("doc('b')", tokens.get(i++));
//		assertEquals("", tokens.get(i++));

	}

	@Test
	public void testNaturalExprInSingleQuotes() {
		
		String source = "'~arg'";
		List<String> tokens = getTokens(source);
		
		int i = 0;
		assertEquals("'", tokens.get(i++));
		assertEquals("arg", tokens.get(i++));
		assertEquals("'", tokens.get(i++));
		
	}

	private List<String> getTokens(String source) {
		List<String> tokens = new ArrayList<String>();
		JapidParser p = new JapidParser(source);
		loop: for (;;) {
			JapidParser.Token state = p.nextToken();
			switch (state) {
			case EOF:
				break loop;
			default:
				String tokenstring = p.getToken();
				tokens.add(tokenstring);
				System.out.println(state.name() + ": [" + tokenstring + "]");
			}
		}
		return tokens;
	}

	@Test
	public void testDollarExpression() {

		final String D = "$";
		final String L = "{";
		final String R = "}";

		String source = "$ $$ $ur	$u $_ $_index3\r\n $\"hello\".length.hi(foo(var+ 's')) etc... $a=='a'";
		// String source = "hello $user.name.toUpperCase()! $user";
		String[] se = new String[] {
				"$ $$ ",
				"ur",
				"	",
				"u",
				" ",
				"_",
				" ",
				"_index3",
				"\r\n ",
				"\"hello\".length.hi(foo(var+ 's'))",
				" etc... ",
				"a=='a'",
		};
		int i = 0;
		String src =
				se[i++] +
						D + se[i++] +
						se[i++] +
						D + se[i++] +
						se[i++] +
						D + se[i++] +
						se[i++] +
						D + se[i++] +
						se[i++] +
						D + se[i++] +
						se[i++] +
						D + se[i++];
		assertEquals(source, src);
		List<String> tokens = new ArrayList<String>();
		JapidParser p = new JapidParser(src);
		loop: for (;;) {
			JapidParser.Token state = p.nextToken();
			switch (state) {
			case EOF:
				break loop;
			default:
				String tokenstring = p.getToken();
				tokens.add(tokenstring);
				System.out.println(state.name() + ": [" + tokenstring + "]");
			}
		}

//		assertEquals(se.length, tokens.size());
		for (i = 0; i < se.length; i++) {
			assertEquals(se[i], tokens.get(i));
		}
	}

	/**
	 * #{tag sdfsdf/} or #{tag sdfsdf} xxx #{/tag}
	 */
	@Test
	public void testTags() {
		String src = " #{tag sdfsdf/}  #{if true} xxx #{/if} #{mytag ccc} yyy #{/} ";
		List<String> tokens = new ArrayList<String>();
		JapidParser p = new JapidParser(src);
		loop: for (;;) {
			JapidParser.Token state = p.nextToken();
			switch (state) {
			case EOF:
				break loop;
			default:
				String tokenstring = p.getToken();
				tokens.add(tokenstring);
				System.out.println(state.name() + ": [" + tokenstring + "]");
			}
		}
	}

	/**
	 * a period at the end is not part of expression
	 */
	@Test
	public void periodAtExprEnd() {
		String src = "$hello.";
		List<String> tokens = new ArrayList<String>();
		JapidParser p = new JapidParser(src);
		loop: for (;;) {
			JapidParser.Token state = p.nextToken();
			switch (state) {
			case EOF:
				break loop;
			default:
				String tokenstring = p.getToken();
				tokens.add(tokenstring);
				System.out.println(state.name() + ": [" + tokenstring + "]");
			}
		}
		assertEquals(3, tokens.size());
		assertEquals("hello", tokens.get(1));

	}

	@Test
	public void scriptTags() {
		String src = "~[ my.java.method()]~ {% another one %}  ~{ and more}~";
		List<String> tokens = new ArrayList<String>();
		JapidParser p = new JapidParser(src);
		loop: for (;;) {
			JapidParser.Token state = p.nextToken();
			switch (state) {
			case EOF:
				break loop;
			default:
				String tokenstring = p.getToken();
				tokens.add(tokenstring);
				System.out.println(state.name() + ": [" + tokenstring + "]");
			}
		}
		assertEquals(7, tokens.size());
		assertEquals(" my.java.method()", tokens.get(1));
		assertEquals(" another one ", tokens.get(3));
		assertEquals(" and more", tokens.get(5));

	}

	@Test
	public void testQuotationMarkSurrounding() {
		String src = "$test";
		List<String> tokens = new ArrayList<String>();
		JapidParser p = new JapidParser(src);
		loop: for (;;) {
			JapidParser.Token state = p.nextToken();
			switch (state) {
			case EOF:
				break loop;
			default:
				String tokenstring = p.getToken();
				tokens.add(tokenstring);
				System.out.println(state.name() + ": [" + tokenstring + "]");
			}
		}
		// assertEquals(7, tokens.size());
		// assertEquals(" my.java.method()", tokens.get(1));
		// assertEquals(" another one ", tokens.get(3));
		// assertEquals(" and more", tokens.get(5));

	}

	/**
	 * test the use of single back-quote char, as escaping script line, similar
	 * to \\ in Java
	 */
	@Test
	public void testLineScript() {
		String src = "`code\n\t`code2 \r\nhtml`code3\nhello ``html`code4\nhello";
		JapidParser p = new JapidParser(src);
		List<TokenPair> tokens = p.allTokens();

		assertEquals(9, tokens.size());
		assertEquals("code", tokens.get(1).source.trim());
		assertEquals("code2", tokens.get(3).source.trim());
		assertEquals("code3", tokens.get(5).source.trim());
		assertEquals("hello `html", tokens.get(6).source.trim());
		assertEquals("code4", tokens.get(7).source.trim());
		assertEquals("hello", tokens.get(8).source.trim());

	}

	@Test
	public void testReverse() {
		String src = "@\n@{appliction.index()}";
		JapidParser p = new JapidParser(src);
		List<TokenPair> tokens = p.allTokens();
		assertEquals(5, tokens.size());
		assertEquals(Token.PLAIN, tokens.get(0).token);
		assertEquals(Token.SCRIPT_LINE, tokens.get(1).token);
		assertEquals(Token.PLAIN, tokens.get(2).token);
		assertEquals(Token.ACTION, tokens.get(3).token);
		assertEquals(Token.PLAIN, tokens.get(4).token);
	}

	@Test
	public void testReverseStatic() {
		String src = "@\n@{'/public/static/stuff'}";
		JapidParser p = new JapidParser(src);
		List<TokenPair> tokens = p.allTokens();
		assertEquals(5, tokens.size());
		assertEquals(Token.PLAIN, tokens.get(0).token);
		assertEquals(Token.SCRIPT_LINE, tokens.get(1).token);
		assertEquals(Token.PLAIN, tokens.get(2).token);
		assertEquals(Token.ACTION, tokens.get(3).token);
		assertEquals(Token.PLAIN, tokens.get(4).token);
	}
	
	@Test
	public void testLineScriptAt() {
		String src = "@code\n\r\t@code2 \r\nhtml@code3\n\rhello `html@code4\nhello";
		JapidParser p = new JapidParser(src);
		List<TokenPair> tokens = p.allTokens();
		
		assertEquals(9, tokens.size());
		assertEquals("code", tokens.get(1).source.trim());
		assertEquals("code2", tokens.get(3).source.trim());
		assertEquals("code3", tokens.get(5).source.trim());
		assertEquals("hello `html", tokens.get(6).source.trim());
		assertEquals("code4", tokens.get(7).source.trim());
		assertEquals("hello", tokens.get(8).source.trim());
		
	}
	
	@Test
	public void testTemplateArgs() {
		String src = "`(String a, a.b.A c)\nhello";
		JapidParser p = new JapidParser(src);
		List<TokenPair> tokens = p.allTokens();
		assertEquals(3, tokens.size());
		assertEquals(Token.TEMPLATE_ARGS, tokens.get(1).token);
	}

	@Test
	public void testTemplateArgsAt() {
		String src = "@(String a, a.b.A c)\nhello";
		JapidParser p = new JapidParser(src);
		List<TokenPair> tokens = p.allTokens();
		assertEquals(3, tokens.size());
		assertEquals(Token.TEMPLATE_ARGS, tokens.get(1).token);
	}
	
	@Test
	public void testBackquoteAlone() {
		String src = "hello \n  `\n ss\n`  ";
		JapidParser p = new JapidParser(src);
		List<TokenPair> tokens = p.allTokens();
//		dumpTokens(tokens);
		assertEquals(4, tokens.size());
		assertEquals(Token.PLAIN, tokens.get(0).token);
		assertEquals(Token.SCRIPT_LINE, tokens.get(1).token);
		assertEquals(Token.PLAIN, tokens.get(2).token);
		assertEquals(Token.SCRIPT_LINE, tokens.get(3).token);
		
		assertEquals("", tokens.get(1).source.trim());
		assertEquals("", tokens.get(3).source.trim());
		
	}

	/**
	 * test optional () in tag invocation
	 */
	@Test
	public void testPatenthesisInTagInocation() {
		String src = "#{tag a, b.goo() | String c/}";
		List<String> tokens = new ArrayList<String>();
		JapidParser p = new JapidParser(src);
		loop: for (;;) {
			JapidParser.Token state = p.nextToken();
			switch (state) {
			case EOF:
				break loop;
			default:
				String tokenstring = p.getToken();
				tokens.add(tokenstring);
				System.out.println(state.name() + ": [" + tokenstring + "]");
			}
		}
	}
	
	@Test
	public void testScriptline() {
		String src = "hello `t Tag2 \"123\"`!";
		JapidParser p = new JapidParser(src);
		List<TokenPair> tokens = p.allTokens();
//		dumpTokens(tokens);
		assertEquals(3, tokens.size());
		assertEquals(Token.PLAIN, tokens.get(0).token);
		assertEquals(Token.SCRIPT_LINE, tokens.get(1).token);
		assertEquals(Token.PLAIN, tokens.get(2).token);
	}

	@Test
	public void testScriptlineWithAt() {
		String src = "@\nhello @t Tag2 \"123\"@!";
		JapidParser p = new JapidParser(src);
		List<TokenPair> tokens = p.allTokens();
		dumpTokens(tokens);
		assertEquals(5, tokens.size());
		assertEquals(Token.PLAIN, tokens.get(0).token);
		assertEquals(Token.SCRIPT_LINE, tokens.get(1).token);
		assertEquals(Token.PLAIN, tokens.get(2).token);
		assertEquals(Token.SCRIPT_LINE, tokens.get(3).token);
		assertEquals(Token.PLAIN, tokens.get(4).token);
	}

	@Test
	public void testAtLiteral() {
		String src = "`@hello";
		JapidParser p = new JapidParser(src);
		List<TokenPair> tokens = p.allTokens();
		dumpTokens(tokens);
		assertEquals(1, tokens.size());
		assertEquals(Token.PLAIN, tokens.get(0).token);
	}

	@Test
	public void testAllLeadingSpaceInline() {
		String src = "  \t";
		assertTrue(JapidParser.allLeadingSpaceInline(src));

		src = "sdf\n  \t";
		assertTrue(JapidParser.allLeadingSpaceInline(src));

		src = "a ";
		assertFalse(JapidParser.allLeadingSpaceInline(src));

		src = "\na ";
		assertFalse(JapidParser.allLeadingSpaceInline(src));

	}

	@Test
	public void testGetCurrentLine() {
		String src = "abc\n def \n ghi";
		assertEquals("abc", JapidParser.getCurrentLine(src, 0));
		assertEquals(" def ", JapidParser.getCurrentLine(src, 4));
		assertEquals(" def ", JapidParser.getCurrentLine(src, 5));
		assertEquals(" def ", JapidParser.getCurrentLine(src, 7));
		assertEquals(" def ", JapidParser.getCurrentLine(src, 7));
		// the newline belongs to the next line
		assertEquals(" ghi", JapidParser.getCurrentLine(src, 10));
		assertEquals(" ghi", JapidParser.getCurrentLine(src, 11));
		assertEquals(" ghi", JapidParser.getCurrentLine(src, src.length() - 1));
	}
	
	@Test
	public void testGetRestLine() {
		String src = "abc\n def \n ghi";
		assertEquals("bc", JapidParser.getRestLine(src, 0));
		assertEquals("def ", JapidParser.getRestLine(src, 4));
		assertEquals("ef ", JapidParser.getRestLine(src, 5));
		assertEquals(" ", JapidParser.getRestLine(src, 7));
		assertEquals("", JapidParser.getRestLine(src, 13));
	}
	
	@Test
	public void testEscapeSpecial() {
		String src = "hello ``, `@, `#, `$, `% `&, `*end";
		String r = JapidParser.escapeSpecialWith(src, '`');
		System.out.println(r); 
		assertEquals("hello `, @, #, $, % %, *end", r);
		assertTrue(r.endsWith("end"));
		src = "hello ~~, ~`, ~@, ~#, ~$, ~% ~&, ~*end";
		r = JapidParser.escapeSpecialWith(src, '~');
		System.out.println(r); 
		assertEquals("hello ~, `, @, #, $, % %, *end", r);
		assertTrue(r.endsWith("end"));
		// continue sign
		src = "hello \\\r\nworld";
		r = JapidParser.escapeSpecialWith(src, '~');
		System.out.println(r); 
		assertEquals("hello world", r);

		src = "hello \\\nworld";
		r = JapidParser.escapeSpecialWith(src, '~');
		System.out.println(r); 
		assertEquals("hello world", r);

		src = "hello \\world";
		r = JapidParser.escapeSpecialWith(src, '~');
		System.out.println(r); 
		assertEquals("hello \\world", r);
	}

	/**
	 * test the parsing of `()
	 */
	@Test
	public void testTemplateArgsWithDefaults() {
		String src = "`(A a, @Default(1)int b \n) \t \n end";
		JapidParser p = new JapidParser(src);
		List<TokenPair> tokens = p.allTokens();
		dumpTokens(tokens);
	}

	/**
	 * test the parsing of `()
	 */
	@Test
	public void testTemplateArgsWithDefaults2() {
		String src = "`( String name, String url, String type, String data, String reRender,\r\n" + 
				"       @default(\"html\") String dataType,\r\n" + 
				"       String beforeSend, String success, String jsData, Boolean cache, String event\r\n" + 
				"   )\n well...";
		JapidParser p = new JapidParser(src);
		List<TokenPair> tokens = p.allTokens();
		dumpTokens(tokens);
	}
		@Test
	public void testContinue() {
		String src = "hello \\\n `t Tag2 \\\n \"123\"`!";
		JapidParser p = new JapidParser(src);
		List<TokenPair> tokens = p.allTokens();
//		dumpTokens(tokens);
		assertEquals(3, tokens.size());
		assertEquals(Token.PLAIN, tokens.get(0).token);
		assertEquals("hello  ", tokens.get(0).source);
		assertEquals(Token.SCRIPT_LINE, tokens.get(1).token);
		assertEquals("t Tag2  \"123\"", tokens.get(1).source);
		assertEquals(Token.PLAIN, tokens.get(2).token);
	}


	/**
	 * @param tokens
	 */
	private void dumpTokens(List<TokenPair> tokens) {
		for (TokenPair tp : tokens) {
			System.out.println(tp.token + ":[" + tp.source + "]");
		}
	}
	
	@Test
	public void testReplaceAll() {
		String src = "$[mary[13]]$ and $[tom]$";
		String res = src.replaceAll(JapidParser.PLACE_HOLDER_PATTERN_S, JapidParser.SUB_PATTERN_S);
		System.out.println(res);
		assertEquals("\"+mary[13]+\" and \"+tom+\"", res);
	}
}
