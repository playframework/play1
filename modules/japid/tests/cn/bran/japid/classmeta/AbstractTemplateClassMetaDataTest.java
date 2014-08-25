/**
 * 
 */
package cn.bran.japid.classmeta;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author bran
 *
 */
public class AbstractTemplateClassMetaDataTest {

	@Test
	public void testIsWhiteSpace() {
		char c = '\t';
		boolean b = Character.isSpaceChar(c);
		Assert.assertFalse(b);

		b = Character.isWhitespace(c);
		Assert.assertTrue(b);
		
	}

}
