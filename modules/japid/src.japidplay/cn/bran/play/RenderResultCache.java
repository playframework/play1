package cn.bran.play;

import play.cache.Cache;
import play.libs.Time;
import play.mvc.Scope.Flash;
import cn.bran.japid.template.RenderResult;

/**
 * We need to have a thread bound variable to control if a cache request should
 * be ignored even if the item might be in cache.
 * 
 * It also define a time-zone that caller should use to refresh the cached item.
 * 
 * XXX: review the thread safety of the flags!!!
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * 
 */
public class RenderResultCache {
	// percentage of ttl used for safe cache retrieval
	// above that an ShoudlRefreshingException raised
	static final double SAFE_TIME_ZONE = 0.9;
	public static final String READ_THRU_FLASH = "j.rtf";

	// to indicate that the ignorecache flag should propagate to the next
	// request
	// this is a super flag for cache control
	private static ThreadLocal<Boolean> ignoreCacheFlash = new ThreadLocal<Boolean>() {
		@Override
		protected Boolean initialValue() {
			return false;
		}
	};

	private static ThreadLocal<Boolean> ignoreCache = new ThreadLocal<Boolean>() {
		@Override
		protected Boolean initialValue() {
//			 System.out.println("init ignoreCache");
			return false;
		}
	};
	// cannot use a local in-memory cache since it's a state the server need to
	// keep, which is contradict to state-less philosophy.
	// private static ConcurrentHashMap<String, CachedItemStatus> cacheTacker =
	// new ConcurrentHashMap<String, CachedItemStatus>();
	private static AltCache altCache;

	public static AltCache getAltCache() {
		return altCache;
	}

	public static void setAltCache(AltCache altCache) {
		RenderResultCache.altCache = altCache;
	}

	/**
	 * flag the cache to ignore cache check for the current thread only
	 * 
	 * @param b
	 */
	public static void setIgnoreCache(boolean b) {
		ignoreCache.set(b);
	}

	/**
	 * flag the cache to ignore cache check for the current thread and for the
	 * next request for the user The state will be saved to Flash scope in the
	 * JapidPlugin. For now this method must be called in action or it will be
	 * too late to save in cookie.
	 * 
	 * @param b
	 */
	public static void setIgnoreCacheInCurrentAndNextReq(boolean b) {
		ignoreCache.set(b);
		ignoreCacheFlash.set(b);
	
	}

	public static boolean shouldIgnoreCache() {
		Boolean should = ignoreCache.get();
		if (should == null) {
			return false;
		} else
			return should;
	}

	/**
	 * set a RenderResult in cache
	 * 
	 * @param key
	 * @param rr
	 * @param ttl
	 */
	public static void set(String key, RenderResult rr, String ttl) {
		long tl = Time.parseDuration(ttl) * 1000L;
		CachedItemStatus cachedItemStatus = new CachedItemStatus(tl);
		cacheset(key, ttl, new CachedRenderResult(cachedItemStatus, rr));
		// cacheTacker.put(key, cachedItemStatus);
	}

	/**
	 * @param key
	 * @param ttl
	 * @param cachedItemStatus
	 */
	private static void cacheset(String key, String ttl, CachedRenderResult rrc) {
		if (altCache != null) {
			altCache.set(key, rrc, ttl);
		} else {
			Cache.set(key, rrc, ttl);
		}
	}

	/**
	 * retrieve a cached RenderResult from the underlying cache implementation.
	 * 
	 * @param key
	 * @return RenderResult instance if the cache has it and it is not expiring
	 *         soon.. Null if it doesn't have it or it will expire soon. In the
	 *         latter case any subsequent get request will get a soon-to-expire
	 *         copy or null if it has expired. It guarantees only one request
	 *         will get a null for near expiration case.
	 * @exception ShouldRefreshException
	 */
	public static RenderResult get(String key) throws ShouldRefreshException {
		if (shouldIgnoreCache())
			return null;

		// CachedItemStatus status = cacheTacker.get(key);
		CachedRenderResult renderResult = cacheget(key);

		if (renderResult == null)
			return null;

		if (renderResult.status.shouldRefresh()) {
			throw new ShouldRefreshException(renderResult);
			// return null;
		} else {
			return renderResult.rr;
		}
	}

	/**
	 * @param key
	 * @return
	 */
	private static CachedRenderResult cacheget(String key) {
		if (altCache != null) {
			return altCache.get(key);
		} else {
			return (CachedRenderResult) Cache.get(key);
		}
	}

	public static boolean shouldIgnoreCacheInCurrentAndNextReq() {
		return ignoreCacheFlash.get();
	}

	public static void delete(String key) {
		if (altCache != null) {
			altCache.delete(key);
		} else {
			Cache.delete(key);
		}
	}

	// public static boolean isRefreshing(String key1) {
	// return cacheTacker.get(key1).isRefreshing();
	// }
}
