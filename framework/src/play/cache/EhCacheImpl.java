package play.cache;

import java.util.HashMap;
import java.util.Map;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import play.Logger;

/**
 * EhCache implementation.
 *
 * <p>Ehcache is an open source, standards-based cache used to boost performance,
 * offload the database and simplify scalability. Ehcache is robust, proven and
 * full-featured and this has made it the most widely-used Java-based cache.</p>
 *
 * Expiration is specified in seconds
 * 
 * @see <a href="http://ehcache.org/">http://ehcache.org/</a>
 *
 */
public class EhCacheImpl implements CacheImpl {

    private static EhCacheImpl uniqueInstance;

    CacheManager cacheManager;

    net.sf.ehcache.Cache cache;

    private static final String cacheName = "play";

    private EhCacheImpl() {
        this.cacheManager = CacheManager.create();
        this.cacheManager.addCache(cacheName);
        this.cache = cacheManager.getCache(cacheName);
    }

    public static EhCacheImpl getInstance() {
        return uniqueInstance;
    }

    public static EhCacheImpl newInstance() {
        uniqueInstance = new EhCacheImpl();
        return uniqueInstance;
    }

    @Override
    public void add(String key, Object value, int expiration) {
        if (cache.get(key) != null) {
            return;
        }
        Element element = new Element(key, value);
        element.setTimeToLive(expiration);
        cache.put(element);
    }

    @Override
    public void clear() {
        cache.removeAll();
    }

    @Override
    public synchronized long decr(String key, int by) {
        Element e = cache.get(key);
        if (e == null) {
            return -1;
        }
        long newValue = ((Number) e.getValue()).longValue() - by;
        Element newE = new Element(key, newValue);
        newE.setTimeToLive(e.getTimeToLive());
        cache.put(newE);
        return newValue;
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
    }

    @Override
    public Object get(String key) {
        Element e = cache.get(key);
        return (e == null) ? null : e.getValue();
    }

    @Override
    public Map<String, Object> get(String[] keys) {
        Map<String, Object> result = new HashMap<>(keys.length);
        for (String key : keys) {
            result.put(key, get(key));
        }
        return result;
    }

    @Override
    public synchronized long incr(String key, int by) {
        Element e = cache.get(key);
        if (e == null) {
            return -1;
        }
        long newValue = ((Number) e.getValue()).longValue() + by;
        Element newE = new Element(key, newValue);
        newE.setTimeToLive(e.getTimeToLive());
        cache.put(newE);
        return newValue;

    }

    @Override
    public void replace(String key, Object value, int expiration) {
        if (cache.get(key) == null) {
            return;
        }
        Element element = new Element(key, value);
        element.setTimeToLive(expiration);
        cache.put(element);
    }

    @Override
    public boolean safeAdd(String key, Object value, int expiration) {
        try {
            add(key, value, expiration);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean safeDelete(String key) {
        try {
            delete(key);
            return true;
        } catch (Exception e) {
            Logger.error(e.toString());
            return false;
        }
    }

    @Override
    public boolean safeReplace(String key, Object value, int expiration) {
        try {
            replace(key, value, expiration);
            return true;
        } catch (Exception e) {
            Logger.error(e.toString());
            return false;
        }
    }

    @Override
    public boolean safeSet(String key, Object value, int expiration) {
        try {
            set(key, value, expiration);
            return true;
        } catch (Exception e) {
            Logger.error(e.toString());
            return false;
        }
    }

    @Override
    public void set(String key, Object value, int expiration) {
        Element element = new Element(key, value);
        element.setTimeToLive(expiration);
        cache.put(element);
    }

    @Override
    public void stop() {
        cacheManager.shutdown();
    }
}
