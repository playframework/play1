package initializertests;

import static org.junit.Assert.*;

import org.junit.Test;

public class InitTests {
	class Bar {
		public Bar(String a2) {
			// Initializer is called here
			System.out.println("super ctor called");
			a = a2;
		}
		
		{
			System.out.println("bar initor called");
		}

		String a = "";
		String b = "";
	}
	class Foo extends Bar{
		{
			System.out.println("foo initor called");
			a = "bar";
			b = "hoo";
		}
		
		public Foo() {
			super("sh");
			// initializer is called here
			System.out.println("foo rest called");
		}
		
		public Foo(String a) {
			super(a);
			// initializer is called here
			System.out.println("foo(x) rest called");
		}
	}
	
	@Test
	public void testDefaultCtor() {
		Foo f = new Foo();
		assertEquals("bar", f.a);
		assertEquals("hoo", f.b);
	}

	@Test
	public void testAltCtor() {
		Foo f = new Foo("tee");
		assertEquals("bar", f.a);
		assertEquals("hoo", f.b);
	}
}
