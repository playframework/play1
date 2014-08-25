package controllers.more;
import java.util.Date;

import play.cache.CacheFor;
import cn.bran.play.JapidController;

public class Portlets extends JapidController {
	/**
	 * 
	 */
	private static final String MY_CACHE_KEY = "my_cache_key";
	@CacheFor("20s")
	public static void index() {
		renderJapid("a", "b");
	}

	public static void panel1(String a) {
		System.out.println("panel1 called");
		renderJapid(a);
	}

	public static void panel2(String b) {
		System.out.println("panel2 called");
		renderJapid(b);
	}

	public static void evict2() {
		// to evict the cached JapidResult result from invoking the panel2 action with argument b 
		evictJapidResultCache(Portlets.class, "panel2", "b");
		System.out.println("panel2 cache evicted");
		index();
	}

	public static void evict3() {
		// to evict the cached JapidResult result from invoking the panel2 action with argument b 
		evictJapidResultCache(Portlets.class, "panel3", "ab");
		System.out.println("panel3 cache evicted");
		index();
	}

	@CacheFor("5s")
	public static void panel3(String whatever) {
		System.out.println("panel3 called");
		renderText("<div>" + new Date() + "</div>");
	}
//
//	@CacheFor(value = "5s", id = MY_CACHE_KEY)
//	public static void panel4(String whatever) {
//		System.out.println("panel4 called");
//		renderText("<div id='myid'>" + new Date() + "</div>");
//	}
//	public static void evict4() {
//		// to evict the cached JapidResult result from invoking the panel2 action with argument b 
//		evictJapidResultCache(MY_CACHE_KEY);
//		System.out.println("panel3 cache evicted");
//		index();
//	}
	
}
