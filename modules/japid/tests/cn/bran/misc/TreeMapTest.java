package cn.bran.misc;

import static org.junit.Assert.*;

import java.util.TreeMap;

import org.junit.Test;

public class TreeMapTest {
	/**
	 * verify that the order of integer index entries is ascending
	 */
	@Test public void testTreeMapOrder() {
		TreeMap<Integer, Object> m = new TreeMap<Integer, Object>();
		
		m.put(10, 12312);
		m.put(100, 12312);
		m.put(20, 12312);
		m.put(1, 12312);
		
		int c = 0;
		for (Integer i : m.keySet()) {
			assertTrue(c < i);
			c = i;
		}
	}
}
