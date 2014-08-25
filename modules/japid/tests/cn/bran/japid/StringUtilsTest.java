package cn.bran.japid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import cn.bran.japid.util.StringUtils;



public class StringUtilsTest {

	@Test
	public void testEncode() throws IOException {
		String src = "自由交往";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteBuffer bb = StringUtils.encodeUTF8(src);
		byte[] array = bb.array();
		int p = bb.position();
		assertTrue(p <= array.length);
		baos.write(array, 0, p);
		baos.flush();
		String out = new String(baos.toByteArray(), "UTF-8");
		assertEquals(src, out);
	}
	
	@Test
	public void testSpeed() throws IOException {
		String src = "自由交往";
		StringBuffer sb = new StringBuffer(10000);
		for (int i = 0; i < 4000; i++) {
			sb.append(src);
		}
		src = sb.toString();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
		long t1 = 0, t2 = 0;
		ByteBuffer bb = StringUtils.encodeUTF8(src);
		bb = StringUtils.encodeUTF8(src);
		bb = StringUtils.encodeUTF8(src);
		bb = StringUtils.encodeUTF8(src);
		baos.write(src.getBytes("UTF-8"));
		baos.write(src.getBytes("UTF-8"));
		baos.write(src.getBytes("UTF-8"));
		
		for (int i = 0; i < 200; i++) {
			{
				baos.reset();
				long t = System.nanoTime();
				bb = StringUtils.encodeUTF8(src);
				byte[] array = bb.array();
				int p = bb.position();
				baos.write(array, 0, p);
				t1 += System.nanoTime() - t;
			}
			{
				baos.reset();
				long t = System.nanoTime();
				baos.write(src.getBytes("UTF-8"));
				t2 += System.nanoTime() - t;
			}
		}
		System.out.println("my encoder took: " + t1);
		System.out.println("String.getBytes() took:: " + t2);
	}
	
	@Test public void testStringWhiteSpace() {
		String src = "\thello \r\n ";
		String r = src.trim();
		assertEquals("hello", r);
	}
	
	@Test
	public void testQueryBuilder() {
		Map<String, Object> hash = new LinkedHashMap<String, Object>();
		hash.put("name", "Bing Ran");
		hash.put("phones", new ArrayList<String>() {{add("123"); add("456");}});
		String q = StringUtils.buildQuery(hash);
		assertEquals("name=Bing+Ran&phones=123&phones=456", q);
//		System.out.println(q);
	}
}
