/**
 * 
 */
package cn.bran.play;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

/**
 * @author bran
 *
 */
public class JapidPlayRendererTest {

	@Test
	public void testGetClassName() {
		File f = new File("japidroot/japidviews/a/b.html");
		String className = JapidPlayRenderer.getClassName(f);
		assertEquals("japidviews.a.b", className);

		f = new File("japidviews/a/b.xml");
		className = JapidPlayRenderer.getClassName(f);
		assertEquals("japidviews.a.b_xml", className);
		
		f = new File("japidroot/japidviews/a/b.js");
		className = JapidPlayRenderer.getClassName(f);
		assertEquals("japidviews.a.b_js", className);
		
	}

}
