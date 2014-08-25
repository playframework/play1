/**
 * 
 */
package cn.bran.play.routing;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author bran
 * 
 */
public class ParamSpecTest {

	@Test
	public void testPattern() {
		String input = "abc";
		String[] r = ParamSpec.extract(input);
		assertEquals("", r[0]);
		assertEquals("abc", r[1]);

		input = "<[a-z]+>abc";
		r = ParamSpec.extract(input);
		assertEquals("[a-z]+", r[0]);
		assertEquals("abc", r[1]);
	}

}
