package cn.bran.japid.template;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import cn.bran.japid.compiler.OpMode;
import cn.bran.japid.util.JapidFlags;

public class JapidRendererTest {

	@Test
	public void testGen() {
		JapidRenderer.init(null, "plainjapid", 1, null);
		JapidRenderer.gen();
	}

	@Test
	public void testReGen() throws IOException {
		JapidRenderer.init(null, "plainjapid", 1, null);
		JapidRenderer.regen();
	}

//	@Test
//	public void testReGenWithPlay() throws IOException {
//		JapidRenderer.init(null, "plainjapid", 1, null, true);
//		JapidRenderer.regen();
//	}

	
//
//	@Test
//	public void testAview() {
//		JapidRenderer.init(OpMode.dev, "plainjapid", 1);
//		String render = new JapidRenderer().render(aview.class, "world");
//		System.out.println(render);
//	}

	@Test
	public void testSmartBindingWithRender() {
		final String UNI = "universe";
		JapidRenderer.init(OpMode.dev, "plainjapid", 1, null);
		String r = new FooControllerBare().a1(UNI);
		System.out.println(r);;
		assertEquals(">" + UNI, r);
	}

	@Test
	public void testRenderSuperMethod() {
		final String UNI = "universe";
		JapidRenderer.init(OpMode.dev, "plainjapid", 1, null);
		String r = new FooController().a1(UNI);
		System.out.println(r);
		assertTrue(r.contains("<head>my view - universe1</head>"));
		assertTrue(r.contains("-> a1-: [universe]-->"));
	}

	@Test
	public void testExplicit() {
		final String UNI = "universe";
		JapidRenderer.init(OpMode.prod, "plainjapid", 1, null);
		String r = new FooControllerBare().a2(UNI);
		System.out.println(r);;
		assertEquals(">" + UNI, r);
	}

	@Test
	public void testExplicit2() {
		final String UNI = "universe";
		JapidRenderer.init(OpMode.prod, "plainjapid", 1, null);
		String r = JapidRenderer.renderWith("cn.bran.japid.template.FooControllerBare.a1", UNI);
		System.out.println(r);;
		assertEquals(">" + UNI, r);
	}

	@Test
	public void testExplicit3() {
		final String UNI = "universe";
		JapidFlags.setLogLevelDebug();
		JapidRenderer.init(OpMode.prod, "plainjapid", 1, null);
		String r = JapidRenderer.renderWith("echo", UNI);
		r = JapidRenderer.renderWith("echo2", UNI);
		r = JapidRenderer.renderWith("echo", UNI);
		System.out.println(r);;
		assertEquals("echo: " + UNI, r);
	}

	@Test
	public void testOpenFor() {
		JapidFlags.setLogLevelDebug();
		JapidRenderer.init(OpMode.prod, "plainjapid", 1, null);
		String r = JapidRenderer.renderWith("open_for");
		System.out.println(r);;
	}
}
