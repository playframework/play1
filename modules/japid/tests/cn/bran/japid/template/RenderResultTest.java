package cn.bran.japid.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import cn.bran.play.AltCacheSimpleImpl;
import cn.bran.play.CacheableRunner;
import cn.bran.play.RenderResultCache;

public class RenderResultTest implements Serializable {
	private static final String KEKKE = "kekke";

	@Test
	public void testExternalizeRenderResult() throws IOException, ClassNotFoundException {
		Map<String, String> map = new HashMap<String, String>();
		map.put("a", "aa");
		map.put("b", "bb");
		StringBuilder sb = new StringBuilder("hello");
		long rt = 1000;
		RenderResult rr = new RenderResult(map, sb, rt);
		byte[] ba = write(rr);
		rr = (RenderResult) read(ba);
		assertNotNull(rr);
		Map<String, String> headers = rr.getHeaders();
		assertEquals(2, headers.size());
		assertEquals("aa", headers.get("a"));
		assertEquals("hello", rr.getContent().toString());
		assertEquals(rt, rr.renderTime);

	}

	@Test
	public void testExternalizeRenderResultPartial() throws IOException, ClassNotFoundException {
		RenderResultCache.setAltCache(new AltCacheSimpleImpl());
		Map<String, String> map = new HashMap<String, String>();
		map.put("a", "aa");
		map.put("b", "bb");
		StringBuilder sb = new StringBuilder("hello");
		long rt = 1000;
		CacheableRunner cr = new CacheableRunner("1m", "anything") {
			@Override
			protected RenderResult render() {
				return null;
			}
		};
		Map<Integer, ActionRunner> runners = new HashMap<Integer, ActionRunner>();
		runners.put(2, cr);
		RenderResultPartial rr = new RenderResultPartial(map, sb, rt, runners);
		byte[] ba = write(rr);
		rr = (RenderResultPartial) read(ba);

		assertNotNull(rr);
		Map<String, String> headers = rr.getHeaders();
		assertEquals(2, headers.size());
		assertEquals("aa", headers.get("a"));
		assertEquals("hello", rr.getText());
		assertEquals(rt, rr.renderTime);
		Map<Integer, ActionRunner> map2 = rr.getActionRunners();
		assertEquals(1, map2.size());
	}
	
	@Test
	public void testBeanWithAnony() {
		BeanInner sb = new BeanInner();
		byte[] ba = write(sb);
		sb = (BeanInner) read(ba);
		assertNotNull(sb);
	}

	@Test
	public void testStringBuilder() {
		StringBuilder sb = new StringBuilder(KEKKE);
		byte[] ba = write(sb);
		sb = (StringBuilder) read(ba);
		assertEquals(KEKKE, sb.toString());
	}
	
	private byte[] write(Object o) {
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(boas);
			oos.writeObject(o);
			oos.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return boas.toByteArray();
	}

	private Object read(byte[] ba) {
		ByteArrayInputStream bais = new ByteArrayInputStream(ba);
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(bais);
			return ois.readObject();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;
	}
}
