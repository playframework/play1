package play.cache;

import play.mvc.Http.Request;

/**
 * Allow custom cache key to be used by applications.
 */
public interface CacheKeyGenerator {
	public String generate(Request request);
}