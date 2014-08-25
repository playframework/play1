package cn.bran.japid;

import static org.junit.Assert.*;

import org.junit.Test;

import cn.bran.japid.compiler.JapidAbstractCompiler;


public class AbstractCompilerTest {

	@Test
	public void testComposeValidMultiLines() {
		String src = "~(\n" + 
		"	bran.Post post, \n" + 
		"	String as\n" + 
		")\n" + 
		"\n";
		
		String result = JapidAbstractCompiler.composeValidMultiLines(src);
		System.out.println("|" + result + "|");
	}

	@Test
	public void testComposeValidMultiLinesSingleLine() {
		String src = " ";
		
		String result = JapidAbstractCompiler.composeValidMultiLines(src);
		assertEquals("\" \"", result);
	}

	@Test
	public void testComposeValidMultiLines2lines() {
		String src = " \n ";
		
		String result = JapidAbstractCompiler.composeValidMultiLines(src);
		System.out.println("|" + result + "|");
		assertEquals("\" \\n\" + \n\" \"", result);
	}
	
}
