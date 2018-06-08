package play.cache;

import play.mvc.Http.Request;

/**
 * The Default Cache Key Generator
 */
public class DefaultCacheKeyGenerator implements CacheKeyGenerator {
	@Override
	public String generate(Request request) {
		return "urlcache:" + request.url + request.querystring;
	}
}