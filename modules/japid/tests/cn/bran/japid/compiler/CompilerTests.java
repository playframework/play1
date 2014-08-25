package cn.bran.japid.compiler;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import org.junit.Test;

import cn.bran.japid.compiler.JapidAbstractCompiler;
import cn.bran.japid.compiler.JapidLayoutCompiler;
import cn.bran.japid.compiler.JapidTemplateCompiler;
import cn.bran.japid.compiler.JavaSyntaxTool;
import cn.bran.japid.template.JapidTemplate;

/**
 * have tests for all three type compilers.
 * 
 * How do I verify the integrity the generated source files?
 * 
 * @author bran
 *
 */
public class CompilerTests {

	@Test
	public void testOpenFor() throws IOException {
		String src = readFile("tests/openFor.html");
		JapidTemplate bt = new JapidTemplate("tests/openFor.html", src);
		JapidAbstractCompiler cp = new JapidLayoutCompiler();
		cp.compile(bt);
		System.out.println(bt.javaSource);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
	}

	@Test
	public void testCompileLayout() throws IOException {
		String src = readFile("JapidSample/app/japidviews/_layouts/Layout.html");
		JapidTemplate bt = new JapidTemplate("tag/Layout.html", src);
		JapidAbstractCompiler cp = new JapidLayoutCompiler();
		cp.compile(bt);
		System.out.println(bt.javaSource);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
	}
	
	@Test
	public void testIfCommand() throws IOException {
		String src = readFile("JapidSample/app/japidviews/Application/ifs.html");
		JapidTemplate bt = new JapidTemplate("Application/ifs.html", src);
		JapidAbstractCompiler cp = new JapidLayoutCompiler();
		cp.compile(bt);
		String javaSource = bt.javaSource;
		System.out.println(javaSource);
		assertTrue(javaSource.contains("if(!asBoolean(ss))"));
		assertTrue(javaSource.contains("else if(!asBoolean(ss))"));
		assertTrue("invalid java code", JavaSyntaxTool.isValid(javaSource));
	}
	
	@Test
	public void testOpenIfCommand() throws IOException {
		String src = readFile("JapidSample/app/japidviews/Application/ifs2.html");
		JapidTemplate bt = new JapidTemplate("Application/ifs2.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler();
		cp.compile(bt);
		System.out.println(bt.javaSource);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
	}
	
	@Test
	public void testLayoutWithArgs() throws IOException {
		String src = readFile("JapidSample/app/japidviews/more/Perf/perfmain.html");
		JapidTemplate bt = new JapidTemplate("more/Perf/perfmain.html", src);
		JapidAbstractCompiler cp = new JapidLayoutCompiler();
		cp.compile(bt);
		System.out.println(bt.javaSource);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
	}
	
	@Test
	public void testExtendsLayoutWithArgs() throws IOException {
		String src = readFile("JapidSample/app/japidviews/more/Perf/perf.html");
		JapidTemplate bt = new JapidTemplate("more/Perf/perf.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler();
		cp.compile(bt);
		System.out.println(bt.javaSource);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
	}
	
	@Test
	public void testReverse() throws IOException {
		String path = "tests/reverse.html";
		String src = readFile(path);
		JapidTemplate bt = new JapidTemplate(path, src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler();
		cp.compile(bt);
		String srccode = bt.javaSource;
		System.out.println(srccode);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(srccode));
		assertTrue(srccode.contains("p(lookupStatic(\"/x/y.html\"))"));
		assertTrue(srccode.contains("p(lookupStatic(\"/a/b/c.html\"))"));
		assertTrue(srccode.contains("p(lookup(\"com.action\", \"ad\"))"));
	}
	
	
	@Test
	public void testAnotherLayout() throws IOException, ParseException {
		String src = readFile("JapidSample/app/japidviews/_layouts/TagLayout.html");
		JapidTemplate bt = new JapidTemplate("japidviews/_layouts/TagLayout.html", src);
		JapidAbstractCompiler cp = new JapidLayoutCompiler();
		cp.compile(bt);
		String srccode = bt.javaSource;
		System.out.println(srccode);
		CompilationUnit cu = JavaSyntaxTool.parse(srccode);
		assertTrue(srccode.contains("package japidviews._layouts;"));
		assertTrue(srccode.contains("public abstract class TagLayout extends cn.bran.play.JapidTemplateBase"));
		assertTrue(srccode.contains("protected abstract void doLayout();"));
		assertTrue(srccode.contains("@Override public void layout()"));
		
	}
	
	@Test
	public void testNoPlayCommand() throws IOException, ParseException {
		String src = readFile("JapidSample/app/japidviews/templates/noplay.html");
		JapidTemplate bt = new JapidTemplate("japidviews/templates/noplay.html", src);
		JapidAbstractCompiler cp = new JapidLayoutCompiler();
		cp.compile(bt);
		String srccode = bt.javaSource;
		System.out.println(srccode);
		CompilationUnit cu = JavaSyntaxTool.parse(srccode);
	}
	
	@Test
	public void testSubLayout() throws IOException {
		String src = readFile("JapidSample/app/japidviews/_layouts/SubLayout.html");
		JapidTemplate bt = new JapidTemplate("japidviews/_layouts/SubLayout.html", src);
		JapidAbstractCompiler cp = new JapidLayoutCompiler();
		cp.compile(bt);
		String srccode = bt.javaSource;
		System.out.println(srccode);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
		assertTrue(srccode.contains("package japidviews._layouts;"));
		assertTrue(srccode.contains("public abstract class SubLayout extends Layout"));
		assertTrue(srccode.contains("protected abstract void doLayout();"));
		assertTrue(srccode.contains("@Override public void layout()"));
		
	}
	
	@Test
	public void testTemplateWithCallbackTagCalls() throws IOException, ParseException {
		String src = readFile("JapidSample/app/japidviews/templates/AllPost.html");

		JapidTemplate bt = new JapidTemplate("japidviews/templates/AllPost.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler();
		cp.compile(bt);
		System.out.println(bt.javaSource);
		CompilationUnit cu = JavaSyntaxTool.parse(bt.javaSource);
		System.out.println(cu);
//		assertTrue("invalid java code", JavaSyntaxValidator.isValid(bt.javaSource));
		
	}
	
	@Test
	public void testOpenForInDef() throws IOException, ParseException {
		String src = readFile("tests/openForInDef.html");
		
		JapidTemplate bt = new JapidTemplate("tests/openForInDef.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler();
		cp.compile(bt);
		System.out.println(bt.javaSource);
		CompilationUnit cu = JavaSyntaxTool.parse(bt.javaSource);
		System.out.println(cu);
//		assertTrue("invalid java code", JavaSyntaxValidator.isValid(bt.javaSource));
		
	}

	@Test
	public void testCompileTagWithDoubleDispatch() throws IOException, ParseException {
		String src = readFile("JapidSample/app/japidviews/_tags/Display.html");
		JapidTemplate bt = new JapidTemplate("tags/Display.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler();
		cp.compile(bt);
		String srcCode = bt.javaSource;
		System.out.println(srcCode);
		
		CompilationUnit cu = JavaSyntaxTool.parse(srcCode);
		assertTrue(srcCode.contains("package tags;"));
		assertTrue(srcCode.contains("public class Display extends TagLayout"));
		assertTrue(srcCode.contains("public cn.bran.japid.template.RenderResult render(models.japidsample.Post post,String as, DoBody body) {"));
		assertTrue(srcCode.contains("public cn.bran.japid.template.RenderResult render(models.japidsample.Post post,String as) {"));
		assertTrue(srcCode.contains("@Override protected void doLayout() {"));
		assertTrue("doBody is not presenting", srcCode.contains("body.render(post.getTitle() + \"!\");"));
		assertTrue(srcCode.contains("public static interface DoBody<A>"));
	}

	
	@Test
	public void testIfindef() throws IOException, ParseException {
		String src = readFile("tests/ifindef.html");
		JapidTemplate bt = new JapidTemplate("tests/ifindef.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler();
		cp.compile(bt);
		String srcCode = bt.javaSource;
//		System.out.println(srcCode);
		assertTrue(!srcCode.contains("_if"));
	}
	
	
	@Test
	public void testElvis() throws IOException, ParseException {
		String src = readFile("tests/elvis.html");
		JapidTemplate bt = new JapidTemplate("tests/elvis.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler();
		cp.compile(bt);
		String srcCode = bt.javaSource;
		System.out.println(srcCode);
	}
	
	@Test
	public void testOpenIf() throws IOException, ParseException {
		String src = readFile("tests/openif.html");
		JapidTemplate bt = new JapidTemplate("tests/openif.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler();
		cp.compile(bt);
		String srcCode = bt.javaSource;
		System.out.println(srcCode);
		CompilationUnit cu = JavaSyntaxTool.parse(srcCode);
	}
	
	@Test
	public void testSwitch() throws IOException, ParseException {
		String src = readFile("tests/switchCase.html");
		JapidTemplate bt = new JapidTemplate("tests/switchCase.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler();
		cp.compile(bt);
		String srcCode = bt.javaSource;
		System.out.println(srcCode);
		CompilationUnit cu = JavaSyntaxTool.parse(srcCode);
	}
	

	@Test
	public void testActionNotation() throws IOException {
		String src = readFile("JapidSample/app/japidviews/templates/Actions.html");
	
		JapidTemplate bt = new JapidTemplate("japidviews/templates/Actions.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler ();
		cp.compile(bt);
		System.out.println(bt.javaSource);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
	}
	
	
	@Test
	public void testActionInvocation() throws IOException {
		String src = readFile("tests/actions.html");
		
		JapidTemplate bt = new JapidTemplate("tests/actions.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler ();
		cp.compile(bt);
		String source = bt.javaSource;
		System.out.println(source);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(source));
		assertTrue(source.contains("MyController.foo()"));
		assertTrue(source.contains("MyController.bar()"));
	}
	
	@Test
	public void testOpenBrace() throws IOException {
		String srcFile = "tests/openBrace.html";
		String src = readFile(srcFile);
		
		JapidTemplate bt = new JapidTemplate(srcFile, src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler ();
		cp.compile(bt);
		System.out.println(bt.javaSource);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
	}
	
	@Test
	public void testTagCalls() throws IOException {
		String srcFile = "tests/tagCalls.html";
		String src = readFile(srcFile);
		
		JapidTemplate bt = new JapidTemplate("tagCalls.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler ();
		cp.compile(bt);
		String code = bt.javaSource;
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
		System.out.println(code);
//		assertTrue(code.contains("((tag)(new tag(getOut()).setActionRunners(getActionRunners()))).render(a)"));
		assertTrue(code.contains("new tag(tagCalls.this).render"));
//		assertTrue(code.contains("((my.tag)(new my.tag(getOut())).setActionRunners(getActionRunners())).render(a, new my.tag.DoBody<String>(){"));
		assertTrue(code.contains("new my.tag(tagCalls.this).render"));
		
	}
	@Test
	public void testRecursiveTags() throws IOException {
		String srcFile = "tests/recursiveTagging.html";
		String src = readFile(srcFile);
		
		JapidTemplate bt = new JapidTemplate("tests/recursiveTagging.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler ();
		cp.compile(bt);
		String code = bt.javaSource;
		System.out.println(code);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
		assertTrue(code.contains("new recursiveTagging(recursiveTagging.this).render"));
	}
	
	@Test
	public void testTagline() throws IOException, ParseException {
		String srcFile = "tests/tagline.html";
		String src = readFile(srcFile);
		
		JapidTemplate bt = new JapidTemplate("tagline.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler ();
		cp.compile(bt);
		String code = bt.javaSource;
		System.out.println(code);
		CompilationUnit cu = JavaSyntaxTool.parse(code);
//		System.out.println(cu);

	}

	@Test
	public void testTagNamedArgsWithBody() throws IOException, ParseException {
		String srcFile = "tests/callTagWithBody.html";
		String src = readFile(srcFile);
		
		JapidTemplate bt = new JapidTemplate("tests/callTagWithBody.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler ();
		cp.compile(bt);
		String code = bt.javaSource;
		assertTrue("invalid java code", JavaSyntaxTool.isValid(code));
		System.out.println(code);
		
	}

	@Test
	public void testLog() throws IOException, ParseException {
		String srcFile = "JapidSample/app/japidviews/templates/log.html";
		String src = readFile(srcFile);
		
		JapidTemplate bt = new JapidTemplate("japidviews/templates/Actions.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler ();
		cp.compile(bt);
		CompilationUnit cu = JavaSyntaxTool.parse(bt.javaSource);
		System.out.println(cu);
	}

	@Test
	public void testVerbatim() throws IOException, ParseException {
		String srcFile = "JapidSample/app/japidviews/Application/verbatim.html";
		String src = readFile(srcFile);
		
		JapidTemplate bt = new JapidTemplate("japidviews/Application/verbatim.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler ();
		cp.compile(bt);
		CompilationUnit cu = JavaSyntaxTool.parse(bt.javaSource);
		System.out.println(cu);
	}

	@Test
	public void testTagBlock() throws IOException {
		String srcFile = "JapidSample/app/japidviews/templates/tagBody.html";
		String src = readFile(srcFile);
		
		JapidTemplate bt = new JapidTemplate("japidviews/templates/tagBody.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler ();
		cp.compile(bt);
		System.out.println(bt.javaSource);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
//		assertTrue(bt.javaSource.contains("((anotherTag)(new anotherTag(getOut())).setActionRunners(getActionRunners())).render(echo, new anotherTag.DoBody<String>(){"));
		assertTrue(bt.javaSource.contains("new moreTag(tagBody.this)"));
	}

	@Test
	public void testElvisEscape() throws IOException {
		String src = "~{ a ?: b}";
		
		JapidTemplate bt = new JapidTemplate("baba.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler ();
		cp.compile(bt);
		System.out.println(bt.javaSource);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
	}

	@Test
	public void testEachDirective() throws IOException {
		String srcFile = "tests/eachTag.html";
		String src = readFile(srcFile);
		
		JapidTemplate bt = new JapidTemplate("eachTag.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler ();
		cp.compile(bt);
		System.out.println(bt.javaSource);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
		assertFalse(bt.javaSource.contains("setActionRunners"));
	}

	@Test
	public void testDoBodyInDef() throws IOException {
		String srcFile = "tests/doBodyInDef.html";
		String src = readFile(srcFile);
		
		JapidTemplate bt = new JapidTemplate("doBodyInDef.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler ();
		cp.compile(bt);
		System.out.println(bt.javaSource);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
	}
	
	@Test
	public void testSetDirective() throws IOException {
		String srcFile = "tests/setTag.html";
		String src = readFile(srcFile);
		
		JapidTemplate bt = new JapidTemplate("tests/setTag.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler ();
		cp.compile(bt);
		System.out.println(bt.javaSource);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
		assertTrue(bt.javaSource.contains("@Override protected void message() {"));
		assertTrue(bt.javaSource.contains("@Override protected void title() {"));
	}
	
	@Test
	public void testGetDirective() throws IOException, ParseException {
		String srcFile = "tests/getTag.html";
		String src = readFile(srcFile);
		
		JapidTemplate bt = new JapidTemplate("tests/getTag.html", src);
		JapidAbstractCompiler cp = new JapidLayoutCompiler();
		cp.compile(bt);
		System.out.println(bt.javaSource);
//		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
		CompilationUnit cu = JavaSyntaxTool.parse(bt.javaSource);
		assertTrue("method is not declared", JavaSyntaxTool.hasMethod(cu, "title", Modifier.PROTECTED, "void", ""));
		assertTrue("method is not declared", JavaSyntaxTool.hasMethod(cu, "footer", Modifier.PROTECTED, "void", ""));
		assertTrue("method is not declared", JavaSyntaxTool.hasMethod(cu, "doLayout", Modifier.PROTECTED | Modifier.ABSTRACT, "void", ""));
		assertTrue("method is never called", JavaSyntaxTool.hasMethodInvocatioin(cu, "title"));
		assertTrue("method is never called", JavaSyntaxTool.hasMethodInvocatioin(cu, "footer"));
		
//		assertTrue(bt.javaSource.contains("@Override protected void message() {"));
//		assertTrue(bt.javaSource.contains("@Override protected void title() {"));
	}

	@Test
	public void testNamedParams() throws IOException, ParseException {
		String srcFile = "tests/namedParam.html";
		String src = readFile(srcFile);
		
		JapidTemplate bt = new JapidTemplate("tests/namedParam.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler();
		cp.compile(bt);
		System.out.println(bt.javaSource);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
		
		assertTrue(bt.javaSource.contains("new person(namedParam.this).render(named(\"name\", \"Bing\"), named(\"age\", foo(18)))"));
//		assertTrue(bt.javaSource.contains("@Override protected void title() {"));
	}

	@Test
	public void testInclude() throws IOException, ParseException {
		String srcFile = "tests/include.html";
		String src = readFile(srcFile);
		
		JapidTemplate bt = new JapidTemplate(srcFile, src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler();
		cp.compile(bt);
		System.out.println(bt.javaSource);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
	}
	
	@Test
	public void testDefDirective() throws IOException, ParseException {
		String srcFile = "JapidSample/app/japidviews/templates/def.html";
		String src = readFile(srcFile);
		
		JapidTemplate bt = new JapidTemplate("japidviews/templates/def.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler();
		cp.compile(bt);
//		System.out.println(bt.javaSource);
//		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
		CompilationUnit cu = JavaSyntaxTool.parse(bt.javaSource);
		System.out.println(cu);
		assertTrue("method is not declared", JavaSyntaxTool.hasMethod(cu, "foo", "public", "String", null));
		assertTrue("method is not declared", JavaSyntaxTool.hasMethod(cu, "foo2", "public", "String", "String"));
		assertTrue("method is not declared", JavaSyntaxTool.hasMethod(cu, "bar", "public", "String", null));
//		assertTrue("method is never called", JavaSyntaxTool.hasMethodInvocatioin(cu, "title"));
//		assertTrue("method is never called", JavaSyntaxTool.hasMethodInvocatioin(cu, "footer"));
		
//		assertTrue(bt.javaSource.contains("@Override protected void message() {"));
//		assertTrue(bt.javaSource.contains("@Override protected void title() {"));
	}
	
	@Test
	public void testSimpleInvoke() throws IOException {
		String srcFile = "tests/simpleInvoke.html";
		String src = readFile(srcFile);
		
		JapidTemplate bt = new JapidTemplate("simpleInvoke.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler ();
		cp.compile(bt);
		String code = bt.javaSource;
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
		System.out.println(code);
		assertTrue(code.contains("actionRunners.put(getOut().length(), new cn.bran.play.CacheablePlayActionRunner(\"\", MyController.class, \"action\", s + \"2\") {"));
		assertTrue(code.contains("MyController.action(s);"));
		assertTrue(code.contains("MyController.action(s + \"2\");"));
		
	}

	@Test
	public void testScriptlineLayout() throws IOException {
		String srcFile = "JapidSample/app/japidviews/more/MyController/scriptlineLayout.html";
		String src = readFile(srcFile);
		
		JapidTemplate bt = new JapidTemplate("scriptlineLayout.html", src);
		JapidAbstractCompiler cp = new JapidLayoutCompiler();
		cp.compile(bt);
		String code = bt.javaSource;
		System.out.println(code);
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
			
	}

	@Test
	public void testOldInvoke() throws IOException {
		String srcFile = "JapidSample/app/japidviews/Application/authorPanel2.html";
		String src = readFile(srcFile);
		
		JapidTemplate bt = new JapidTemplate("simpleInvoke.html", src);
		JapidAbstractCompiler cp = new JapidTemplateCompiler ();
		cp.compile(bt);
		String code = bt.javaSource;
		assertTrue("invalid java code", JavaSyntaxTool.isValid(bt.javaSource));
		System.out.println(code);
	}
	
	private static String readFile(String path) throws IOException {
		FileInputStream stream = new FileInputStream(new File(path));
		try {
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
			/* Instead of using default, pass in a decoder. */
			return Charset.forName("UTF-8").decode(bb).toString();
		} finally {
			stream.close();
		}
	}
	

}
