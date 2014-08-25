package cn.bran.japid;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class TestListMethodMatch {

	/**
	 * method is defined taking List<String>, am trying to match with
	 * ArrayList<String>
	 * 
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	@Test
	public void testLists() throws SecurityException, NoSuchMethodException {
		List<String> ls = new ArrayList<String>();
		try {
			A.class.getMethod("foo", ls.getClass());
		} catch (NoSuchMethodException e) {
		}

		B.class.getMethod("foo", ls.getClass());

	}

	static class A {
		public void foo(List<String> l) {
		}
	}

	static class B {
		public void foo(ArrayList<String> l) {
		}
	}
}
