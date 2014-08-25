package cn.bran.misc;

import java.lang.reflect.Method;

public class VirtualFunctionVsReflection {

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		Blah x = new Blah();
		ComplexClass cc = new ComplexClass();
		test(cc, cc);
	}

	/**
	 * this is to show that direct call is faster than interface call which is way faster than reflection
	 * @param x
	 * @param cc
	 */
	public static void test(MyInterface x, ComplexClass cc) {
		long start, end;
		long time1 = 0, time2 = 0, time3 = 0;
		int numToDo = 1000000;
		MyInterface interfaceClass = (MyInterface) x;

		// warming up the cache
//		for (int i = 0; i < numToDo; i++) {
//			cc.doSomething(i); // calls a method directly
//		}
		start = System.currentTimeMillis();
		for (int i = 0; i < numToDo; i++) {
			cc.doSomething(i); // calls a method directly
		}
		end = System.currentTimeMillis();
		time1 = end - start;
//
		start = System.currentTimeMillis();
		for (int i = 0; i < numToDo; i++) {
			interfaceClass.doSomething(i); // casts an object to an interface
			// then calls the method
		}
		end = System.currentTimeMillis();
		time2 = end - start;

		
		try {
			Class xClass = x.getClass();
			Class[] argTypes = { int.class };
			Method m = xClass.getMethod("doSomething", argTypes);
			Object[] paramList = new Object[1];
			start = System.currentTimeMillis();
			for (int i = 0; i < numToDo; i++) {
				paramList[0] = i;
				m.invoke(x, paramList); // calls via reflection
			}
			end = System.currentTimeMillis();
			time3 = end - start;

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		System.out.println("calling a method directly: " + time1);
		System.out.println("calling a method directly from an object cast to an interface: " + time2);
		System.out.println("calling a method through reflection: " + time3);
	}
}
