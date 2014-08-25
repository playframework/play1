package cn.bran.japid.compiler;

import static org.junit.Assert.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.Node;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.type.Type;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import org.junit.Test;

import cn.bran.japid.compiler.JavaSyntaxTool.CodeNode;

public class JavaSyntaxToolTest {
	@Test
	public void testAddFinalToAllParams() {
		String src = "String a, final int b, MyObject[] c";
		String finals = JavaSyntaxTool.addFinalToAllParams(src);
		assertEquals("final String a, final int b, final MyObject[] c", finals);
	}

	@Test
	public void testMatchLongestPossibleExpr() {
		String src = "a + b(\"s\".length()) c";
		String finals = JavaSyntaxTool.matchLongestPossibleExpr(src);
		assertEquals("a + b(\"s\".length())", finals);

		src = "a + b()c";
		finals = JavaSyntaxTool.matchLongestPossibleExpr(src);
		assertEquals("a + b()", finals);

		src = "a  ba";
		finals = JavaSyntaxTool.matchLongestPossibleExpr(src);
		assertEquals("a", finals);

		src = "a | 2()";
		finals = JavaSyntaxTool.matchLongestPossibleExpr(src);
		assertEquals("a | 2", finals);

		src = "\"String\".length * 12";
		finals = JavaSyntaxTool.matchLongestPossibleExpr(src);
		assertEquals("\"String\".length * 12", finals);

		src = "12.0";
		finals = JavaSyntaxTool.matchLongestPossibleExpr(src);
		assertEquals("12.0", finals);

		src = "12. a";
		finals = JavaSyntaxTool.matchLongestPossibleExpr(src);
		assertEquals("12.", finals);

		src = "a + b().";
		finals = JavaSyntaxTool.matchLongestPossibleExpr(src);
		assertEquals("a + b()", finals);

		src = "a + b!";
		finals = JavaSyntaxTool.matchLongestPossibleExpr(src);
		assertEquals("a + b", finals);

		src = " ";
		finals = JavaSyntaxTool.matchLongestPossibleExpr(src);
		assertEquals("", finals);

		src = "a;";
		finals = JavaSyntaxTool.matchLongestPossibleExpr(src);
		assertEquals("a", finals);

		src = "a b c a b c a b c a b c a b c a b c a b c a b c a b c a b c a b c "
				+ "a b c a b c a b c a b c a b c a b c a b c a b c a b c a b c a b c "
				+ "a b c a b c a b c a b c a b c a b c a b c a b c a b c a b c a b c "
				+ "a b c a b c a b c a b c a b c a b c a b c a b c a b c a b c a b c "
				+ "a b c a b c a b c a b c a b c a b c a b c a b c a b c a b c a b c "
				+ "a b c a b c a b c a b c a b c a b c a b c a b c a b c a b c a b c "
				+ "a b c a b c a b c a b c a b c a b c a b c a b c a b c a b c a b c ";
		finals = JavaSyntaxTool.matchLongestPossibleExpr(src);
		assertEquals("a", finals);
	}

	@Test
	public void testWeirdExpr() {
		String ex = "\"hello\".hi(foo(var+ \"sd\"))";
		String src = ex + "etc... ~a=='a'";
		String finals = JavaSyntaxTool.matchLongestPossibleExpr(src);
		assertEquals(ex, finals);

	}

	@Test
	public void testHasMathod() throws ParseException {
		String src = "class A { private static void foo(int i, B b); }";
		CompilationUnit cu = JavaSyntaxTool.parse(src);
		assertTrue(JavaSyntaxTool.hasMethod(cu, "foo", "static private",
				"void", " int , B "));
	}

	@Test
	public void testaddParamNamesPlaceHolder() {
		String src = "int  String Object";
		String pama = JavaSyntaxTool.addParamNamesPlaceHolder(src);
		List<Parameter> parseParams = JavaSyntaxTool.parseParams(pama);
		assertEquals(3, parseParams.size());
		assertEquals("int", parseParams.get(0).getType().toString());
		assertEquals("String", parseParams.get(1).getType().toString());
		assertEquals("Object", parseParams.get(2).getType().toString());

		src = "int,  String Object";
		pama = JavaSyntaxTool.addParamNamesPlaceHolder(src);
		parseParams = JavaSyntaxTool.parseParams(pama);
		assertEquals(3, parseParams.size());
		assertEquals("int", parseParams.get(0).getType().toString());
		assertEquals("String", parseParams.get(1).getType().toString());
		assertEquals("Object", parseParams.get(2).getType().toString());
	}

	@Test
	public void testParseParams() {
		String src = "@ Default (3 ) int i,  @Default(foo()+ \"aa\")String s, String m, @Default(\"aa\")String ss";
		List<Parameter> pama = JavaSyntaxTool.parseParams(src);
		Parameter p = pama.get(0);
		String def = JavaSyntaxTool.getDefault(p);
		assertEquals("3", def);
		// System.out.println(def);

		p = pama.get(1);
		def = JavaSyntaxTool.getDefault(p);
		assertEquals("foo() + \"aa\"", def);

		p = pama.get(2);
		def = JavaSyntaxTool.getDefault(p);
		assertNull(def);

		p = pama.get(3);
		def = JavaSyntaxTool.getDefault(p);
		assertEquals("\"aa\"", def);

		// System.out.println(def);
	}

	@Test
	public void testParseParams2() {
		String src = "@default(\"html\") String dataType";
		List<Parameter> pama = JavaSyntaxTool.parseParams(src);
		// System.out.println(def);
	}

	@Test
	public void testBoxPrimitiveTypesInParams() {
		String src = "String a, int b, long[] c, long d";
		String res = JavaSyntaxTool.boxPrimitiveTypesInParams(src);
		assertEquals("String a, Integer b, long[] c, Long d", res);
		
		try {
			src = "d";
			res = JavaSyntaxTool.boxPrimitiveTypesInParams(src);
			fail("should have an exception");
		} catch (RuntimeException e) {
			System.out.println(e);
		}
		
	}

	/**
	 * doBody a, b -> c
	 */
	@Test
	public void testAsClausePattern() {
		Pattern m = JavaSyntaxTool.AS_PATTERN;

		String s1 = "a, 'sfsdf', 123 -> var";
		Matcher matcher = m.matcher(s1);
		assertTrue(matcher.matches());
		// matcher.find();
		assertEquals("a, 'sfsdf', 123 ", matcher.group(1));
		assertEquals("var", matcher.group(2));

		s1 = "a, 'sfsdf', asr - 1";
		matcher = m.matcher(s1);
		assertTrue(!matcher.matches());
	}

	@Test
	public void testAsClauseExtraction() {
		String s = "a, 1 -> c";
		String[] r = JavaSyntaxTool.breakArgParts(s);
		assertEquals(2, r.length);
		assertEquals("a, 1 ", r[0]);
		assertEquals("c", r[1]);

		s = "a, 1";
		r = JavaSyntaxTool.breakArgParts(s);
		assertEquals(1, r.length);
		assertEquals("a, 1", r[0]);

		s = "-> c";
		r = JavaSyntaxTool.breakArgParts(s);
		assertEquals(2, r.length);
		assertEquals("", r[0]);
		assertEquals("c", r[1]);

	}

	@Test
	public void testParsingSimpleArgs() {
		String src = "a, 1, \"hello\", foo()";
		List<String> args = JavaSyntaxTool.parseArgs(src);
		assertEquals(4, args.size());
	}

	@Test
	public void testParsingArgsWithAssignment() {
		String src = "a, b = foo(), 123";
		List<String> args = JavaSyntaxTool.parseArgs(src);
		assertEquals(3, args.size());
	}

	@Test
	public void testParsingInvalidArgs() {
		String src = "a, foo(";
		try {
			List<String> args = JavaSyntaxTool.parseArgs(src);
			fail("should not be here");
		} catch (RuntimeException e) {
			System.out.println(e);
		}
	}

	@Test
	public void testParsingNamedArgsNone() {
		String src = "a, b";
		List<NamedArg> args = JavaSyntaxTool.parseNamedArgs(src);
		assertEquals(0, args.size());
	}

	@Test
	public void testParsingNamedArgsAll() {
		String src = "a = 1, b = foo()";
		List<NamedArg> args = JavaSyntaxTool.parseNamedArgs(src);
		assertEquals(2, args.size());
	}

	@Test
	public void testParsingNamedArgsMixed() {
		String src = "a, b = foo()";
		try {
			List<NamedArg> args = JavaSyntaxTool.parseNamedArgs(src);
			fail("should have caught mixed cases");
		} catch (RuntimeException e) {
			System.out.println(e);
		}

	}
	
	@Test
	public void testParsingNamedArgsInvalid() {
		String src = "a, b = ";
		try {
			List<NamedArg> args = JavaSyntaxTool.parseNamedArgs(src);
			fail("should have caught bad syntax");
		} catch (RuntimeException e) {
			System.out.println(e);
		}
		
	}
	
	
	@Test
	public void testValidMethDecl() {
		String src = "foo(int i, String s)";
		JavaSyntaxTool.isValidMethDecl(src);
		
	}
	
	
	
	@Test
	public void testInvalidMethDecl() {
		String src = "foo(i,  s)";
		try {
			JavaSyntaxTool.isValidMethDecl(src);
			fail("should have thrown exception");
		} catch (RuntimeException e) {
			System.out.println(e);
		}
		
	}
	
	@Test
	public void testMethCall() {
		String src = "foo(i,  s)";
		JavaSyntaxTool.isValidMethodCall(src);

		src = "a.b$.foo (i,  s)";
		JavaSyntaxTool.isValidMethodCall(src);

		src = "foo(i,  String s)";
		try {
			JavaSyntaxTool.isValidMethodCall(src);
			fail("should have thrown an exception");
		} catch (Exception e) {
			System.out.println(e);
		}
		
		try {
			JavaSyntaxTool.isValidMethodCall("int i");
			fail("should have thrown an exception");
		} catch (Exception e) {
			System.out.println(e);
		}
		
		try {
			JavaSyntaxTool.isValidMethodCall("foo(); bar()");
			fail("should have thrown an exception");
		} catch (Exception e) {
			System.out.println(e);
		}
		
		try {
			JavaSyntaxTool.isValidMethodCall("xxx yyy");
			fail("should have thrown an exception");
		} catch (Exception e) {
			System.out.println(e);
		}
		
		try {
			JavaSyntaxTool.isValidMethodCall("int i = 0");
			fail("should have thrown an exception");
		} catch (Exception e) {
			System.out.println(e);
		}
		
		try {
			src = "a.b$.fo o(i,  s)";
			JavaSyntaxTool.isValidMethodCall(src);
			fail("should have thrown an exception");
		} catch (Exception e) {
			System.out.println(e);
		}
		
		src = "a.b";
		try {
			JavaSyntaxTool.isValidMethodCall(src);
			fail("should have thrown an exception");
		} catch (Exception e) {
			System.out.println(e);
		}
		
		src = "a = b()";
		try {
			JavaSyntaxTool.isValidMethodCall(src);
			fail("should have thrown an exception");
		} catch (Exception e) {
			System.out.println(e);
		}
		
	}
	

	@Test
	public void testVarDecl() {
		assertTrue(JavaSyntaxTool.isValidSingleVarDecl("int a"));
		assertTrue(JavaSyntaxTool.isValidSingleVarDecl("String as "));
		assertTrue(JavaSyntaxTool.isValidSingleVarDecl("MyObject[] asd"));

		assertFalse(JavaSyntaxTool.isValidSingleVarDecl("a"));
		assertFalse(JavaSyntaxTool.isValidSingleVarDecl("a b c"));
		assertFalse(JavaSyntaxTool.isValidSingleVarDecl("a, b"));
		assertFalse(JavaSyntaxTool.isValidSingleVarDecl("int a; int b"));
	}
	
	@Test
	public void testForPredicates(){
		String s = "MyType v : myCollection";
		boolean good = JavaSyntaxTool.isValidForLoopPredicate(s);
		assertTrue(good);

		s = "MyType v : myCollection.a.b(\"name: myname\")";
		good = JavaSyntaxTool.isValidForLoopPredicate(s);
		assertTrue(good);

		s = "MyType v  myCollection.a.b";
		good = JavaSyntaxTool.isValidForLoopPredicate(s);
		assertFalse(good);

		s = "v:  myCollection";
		good = JavaSyntaxTool.isValidForLoopPredicate(s);
		assertFalse(good);
	}
	
	@Test
	public void testParseCode(){
		String code = "class A {{for (Type i : ii.a.foo()){}}}";
		List<CodeNode> nodes = JavaSyntaxTool.parseCode(code);
		for (CodeNode n : nodes) {
			System.out.println(n.nestLevel + ":" + n.node.getClass().getName() + ": " + n.node.toString());
		}
	
		code = "class A {{a.foo(); bar(); int i;}}";
		nodes = JavaSyntaxTool.parseCode(code);
		listNodes(nodes);
	}

	private void listNodes(List<CodeNode> nodes) {
		for (CodeNode n : nodes) {
			System.out.println(n.nestLevel + ":" + n.node.getClass().getName() + ": " + n.node.toString());
		}
	}

	@Test
	public void testSingleMethCall(){
		String code = "class A {{a.foo(bar());}}";
		List<CodeNode> nodes = JavaSyntaxTool.parseCode(code);
		assertTrue(nodes.get(5).node instanceof MethodCallExpr);
	}
	
	@Test
	public void testOrExpression(){
		String code = "class A {{foo(a, b |c);}}";
		List<CodeNode> nodes = JavaSyntaxTool.parseCode(code);
		listNodes(nodes);
	}
	
	@Test
	public void testIf() {
		String 
		s = "(foo().bar)";
		assertTrue(JavaSyntaxTool.isIf(s));
		
		s = "(foo().bar) {";
		assertTrue(JavaSyntaxTool.isIf(s));

		s = "(\"sdfsdf\") {";
		assertTrue(JavaSyntaxTool.isIf(s));

		s = "foo().bar ";
		assertFalse(JavaSyntaxTool.isIf(s));

		s = "foo().bar() ";
		assertFalse(JavaSyntaxTool.isIf(s));
		
	}

	@Test
	public void testOpenIf() {
		String 
		s = "foo().bar";
		assertTrue(JavaSyntaxTool.isOpenIf(s));
		
		s = "foo().bar {";
		assertTrue(JavaSyntaxTool.isOpenIf(s));

		s = "(foo).bar() {";
		assertTrue(JavaSyntaxTool.isOpenIf(s));
		
		s = "(foo).bar() ";
		assertTrue(JavaSyntaxTool.isOpenIf(s));
		
		s = "foo().bar++ ";
		assertTrue(JavaSyntaxTool.isOpenIf(s));
		
	}
}
