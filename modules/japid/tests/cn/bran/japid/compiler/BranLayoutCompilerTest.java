package cn.bran.japid.compiler;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Test;

import cn.bran.japid.compiler.JapidAbstractCompiler;
import cn.bran.japid.compiler.JapidLayoutCompiler;
import cn.bran.japid.template.JapidTemplate;



/**
 * 
 * @author bran
 * @deprecated now is on the {@code BranCompilerTests}
 */
public class BranLayoutCompilerTest {

	@Test
	public void testHop() throws IOException {
		FileInputStream fis = new FileInputStream("JapidSample/app/japidviews/_layouts/Layout.html");
		InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
		BufferedReader br = new BufferedReader(isr);
		String src = "";
		for(String line = br.readLine(); line != null; line = br.readLine()) {
			src += line + "\n";
		}
		
		JapidTemplate bt = new JapidTemplate("tag/Layout.html", src);
		JapidAbstractCompiler cp = new JapidLayoutCompiler();
		cp.compile(bt);
		System.out.println(bt.javaSource);
	}
}
