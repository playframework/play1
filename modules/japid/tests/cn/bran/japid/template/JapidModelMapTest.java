package cn.bran.japid.template;

import static org.junit.Assert.*;

import org.junit.Test;

public class JapidModelMapTest {

	@Test
	public void testBuildArgs() {
		JapidModelMap model = new JapidModelMap().put("a", "String").put("b", 68);
		String[] args = new String[] {"a", "b", "c"};
		Object[] ret = model.buildArgs(args);
		assertEquals(3, ret.length);
		assertEquals("String", ret[0]);
		assertEquals(68, ret[1]);
		assertNull(ret[2]);
		
	}

}
