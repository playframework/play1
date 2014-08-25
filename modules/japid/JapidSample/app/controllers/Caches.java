package controllers;

import java.util.HashMap;

import play.cache.Cache;
import cn.bran.japid.template.RenderResult;
import cn.bran.play.CachedItemStatus;
import cn.bran.play.CachedRenderResult;
import cn.bran.play.JapidController;
/**
 *  ! can we implement a cache sharding in the Cache impl? how about multiple key in one batch? A join fetch across multiple servers?
 *  
 *  
 * @author Bing Ran<bing_ran@hotmail.com>
 *
 */
public class Caches extends JapidController {
	private static final String CIS = "CachedItemStatus";
	private static final String KEY = "thisisarelativelylongkey";

	public static void __init(int total) {
		long t = System.currentTimeMillis();
		for (int i = 0; i < total; i++) {
			String k = KEY + i;
//			System.out.println(k);
			Cache.safeAdd(k, k, "10min");
		}
		String x = total + " cache init took: " + (System.currentTimeMillis() - t) + "ms";
		renderText(x);
//		System.out.println(x);
	}

	public static void find(int k) {
//		int total = 100000;
		long t = System.currentTimeMillis();
//		for (int i = 0; i < total; i++) {
//			String k = KEY + i;
//			Cache.add(k, k);
//		}
		Object o = "|";
		for(int i = 1000; i-- > 0;) {
			String key2 = KEY + (k + i);
//			System.out.println(key2);
			o = Cache.get(key2);
		}

		String x = o + " found. took: " + (System.currentTimeMillis() - t) + "ms";
		renderText(x);
//		System.out.println(x);
	}

	public static void f2(int k) {
//		int total = 100000;
		long t = System.currentTimeMillis();
//		for (int i = 0; i < total; i++) {
//			String k = KEY + i;
//			Cache.add(k, k);
//		}
		String key2 = KEY + k;
		System.out.println(key2);
		Object o = Cache.get(key2);
		String x = o + " found. took: " + (System.currentTimeMillis() - t) + "ms";
		renderText(x);
//		System.out.println(x);
	}
	
	public static void testCachedItemStatus() {
		CachedItemStatus cis = new CachedItemStatus(-1);
		Cache.safeAdd(CIS, cis, "10s");
		CachedItemStatus o = (CachedItemStatus) Cache.get(CIS);
		if(o.isExpired())
			renderText("good to know..");
		else
			renderText("bad to know..");
			
	}
	
	public static void testCachedRenderResult() {
		CachedItemStatus cis = new CachedItemStatus(0);
		HashMap<String, String> headers = new HashMap<String, String>();
		RenderResult rr = new RenderResult(headers, new StringBuilder(), 123);
		CachedRenderResult crr = new CachedRenderResult(cis, rr);
		Cache.safeAdd("crr", crr, "10s");
		crr = (CachedRenderResult) Cache.get("crr");
		if(crr.isExpired())
			renderText("good to know..");
		else
			renderText("bad to know..");
	}

	public static void testStringBuilder() {
		String str = "hello";
		StringBuilder sb = new StringBuilder(str);
		String key2 = "sb";
		Cache.safeAdd(key2, sb, "10s");
		sb = (StringBuilder) Cache.get(key2);
		
		if (str.equals(sb.toString()))
			renderText("StringBuilder works...");
		else
			renderText("StringBuilder does not works...");
	}
	
	public static void testRenderResult() {
		HashMap<String, String> headers = null; //new HashMap<String, String>();
		RenderResult rr = new RenderResult(headers, new StringBuilder(), 123);
//		RenderResult rr = new RenderResult(headers, null, 123);
		Cache.safeAdd("crr", rr, "10s");
		rr = (RenderResult) Cache.get("crr");
		if (rr != null)
			renderText("good to know 2...");
		else {
			renderText("bad to know 2...");
		}
		
	}
	
	public void testAction() {
		renderJapid();
	}
}
