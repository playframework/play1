package cn.bran.japid.compiler;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.stmt.ExpressionStmt;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import org.junit.Test;

import cn.bran.japid.compiler.JavaSyntaxTool.Param;

public class JavaSyntaxValidatorTest {
	@Test
	public void testBuildValidAst() {
		String validSrc = "package testdir.d1;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				" Boo 不不 = new Boo('不'); \n" +
				"}\r\n" +
				"";
		CompilationUnit cu = null;
		try {
			cu = JavaSyntaxTool.parse(validSrc);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertNotNull(cu);
	}

	@Test
	public void testParamList() throws UnsupportedEncodingException, ParseException {
		String src = "class A { void m  (String[ ] a, cc.B f) {m(1 + \"s\", 2);}}";
		ByteArrayInputStream in = new ByteArrayInputStream(src.getBytes("UTF-8"));
		CompilationUnit cu = JavaParser.parse(in, "UTF-8");
		assertNotNull(cu);
		// System.out.println(cu.toString());
		new MethodVisitor().visit(cu, null);

	}

	private static class MethodVisitor extends VoidVisitorAdapter {
		@Override
		public void visit(Parameter n, Object arg) {
			// TODO Auto-generated method stub
			System.out.println("p:" + n);
			super.visit(n, arg);
		}

		@Override
		public void visit(ExpressionStmt n, Object arg) {
			System.out.println("e:" + n);
			super.visit(n, arg);
		}

		@Override
		public void visit(MethodCallExpr n, Object arg) {
			System.out.println("m:" + n);
			super.visit(n, arg);
		}

		@Override
		public void visit(MethodDeclaration n, Object arg) {
			// here you can access the attributes of the method.
			// this method will be called for all methods in this
			// CompilationUnit, including inner class methods
			System.out.println(n);
			super.visit(n, arg);
		}
	}

	@Test(expected = ParseException.class)
	public void testBuildInvalidAst() throws ParseException {
		String validSrc = "package testdir.d1;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"hello world" +
				"}\r\n" +
				"";
		try {
			CompilationUnit cu = JavaSyntaxTool.parse(validSrc);
		} catch (ParseException e) {
//			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * test parse parameter list in method declaration
	 */
	@Test
	public void testParseParams() {
		String src = "String[] strings, a.B b";
		List<Parameter> args = JavaSyntaxTool.parseParams(src);
		assertEquals(2, args.size());
		assertEquals("String[]", args.get(0).getType().toString());
		assertEquals("strings", args.get(0).getId().toString());
		assertEquals("a.B", args.get(1).getType().toString());
		assertEquals("b", args.get(1).getId().toString());

		src = "String[] strings, a.B b xx";
		try {
			JavaSyntaxTool.parseParams(src);
			fail("shoudl report invlid pram list");
		} catch (RuntimeException e) {
			;
		}
	}

	/**
	 * test parsing the argument list for method invocation
	 */
	@Test
	public void testParseArgs() {
		String src = "a, b + 123, foo('d', \"hello\")";
		List<String> args = JavaSyntaxTool.parseArgs(src);
		
		for (String a: args) {
			System.out.println(a);
		}
		
		assertEquals(3, args.size());
		assertEquals("a", args.get(0));
		assertEquals("b + 123", args.get(1));
		assertEquals("foo('d', \"hello\")", args.get(2));

		src = "(a, b + 123, foo('d', \"hello\"))";
		args = JavaSyntaxTool.parseArgs(src);
		
		for (String a: args) {
			System.out.println(a);
		}
		
		assertEquals(3, args.size());
		assertEquals("a", args.get(0));
		assertEquals("b + 123", args.get(1));
		assertEquals("foo('d', \"hello\")", args.get(2));

		src = "(int)a, b + 123, foo('d', \"hello\")";
		args = JavaSyntaxTool.parseArgs(src);
		
		for (String a: args) {
			System.out.println(a);
		}
		
		assertEquals(3, args.size());
		assertEquals("(int) a", args.get(0));
		assertEquals("b + 123", args.get(1));
		assertEquals("foo('d', \"hello\")", args.get(2));

		src = "(inta, b + 123, foo('d', \"hello\")";
		try {
			args = JavaSyntaxTool.parseArgs(src);
			fail("should tell a bad grammar");
		} catch (Exception e) {
		}
	}

	@Test
	public void testParseNamedArgs() {
		String src = "a1 =a, a2= b + 123, a3 = foo('d', \"hello\")";
		List<NamedArg> args = JavaSyntaxTool.parseNamedArgs(src);
		
		for (NamedArg a: args) {
			System.out.println(a);
		}
		
		assertEquals(3, args.size());
		assertEquals("a1", args.get(0).name);
		assertEquals("a", args.get(0).valExpr);
		assertEquals("b + 123", args.get(1).valExpr);
		assertEquals("foo('d', \"hello\")", args.get(2).valExpr);

		// invalid format
		src = "a1, a2= b";
		try {
			args = JavaSyntaxTool.parseNamedArgs(src);
			fail("shoudl have thrown an exception");
		} catch (Exception e) {
			System.out.println(e);
		}
		
	}
}
