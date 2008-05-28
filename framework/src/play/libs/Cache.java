package play.libs;

import java.util.HashMap;
import java.util.Map;

public class Cache {

    private static CacheImpl cacheImpl = new LocalCacheImpl();

    public static Boolean add(String key, Object value, String expiration) {
        return cacheImpl.add(key, value, Utils.Time.parseDuration(expiration));
    }

    public static Boolean add(String key, Object value) {
        return cacheImpl.add(key, value, Utils.Time.parseDuration(null));
    }

    public static Object get(String key) {
        return cacheImpl.get(key);
    }

    public static Object remove(String key) {
        return cacheImpl.remove(key);
    }

    public static void clear() {
        cacheImpl.clear();
    }

    public static <T> T get(String key, Class<T> clazz) {
        return (T) cacheImpl.get(key);
    }
    
    static interface CacheImpl {
        public Boolean add(String key, Object value, Long expiration);
        public Object get(String key);
        public void clear();
        public Object remove(String key);
    }

    static class LocalCacheImpl implements CacheImpl {

        private Map<String, CachedElement> cache = new HashMap<String, CachedElement>();

        public Boolean add(String key, Object value, Long expiration) {
            Object v = get(key);
            if (v == null) {
                cache.put(key, new CachedElement(key, value, expiration));
                return true;
            } else {
                return false;
            }
        }

        public Object get(String key) {
            CachedElement cachedElement = cache.get(key);
            if (cachedElement != null && System.currentTimeMillis() >= cachedElement.getExpiration()) {
                cache.remove(key);
                return null;
            }
            return cachedElement == null ? null : cachedElement.getValue();
        }

        public Object remove(String key) {
            CachedElement cachedElement = cache.get(key);
            if (cachedElement != null) {
                cache.remove(key);
                return cachedElement.getValue();
            }
            return null;
        }

        public void clear() {
            cache.clear();
        }
        //
        class CachedElement {

            private String key;
            private Object value;
            private Long expiration;

            public CachedElement(String key, Object value, Long expiration) {
                this.key = key;
                this.value = value;
                this.expiration = expiration;
            }

            public String getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = key;
            }

            public Object getValue() {
                return value;
            }

            public void setValue(Object value) {
                this.value = value;
            }

            public Long getExpiration() {
                return expiration;
            }

            public void setExpiration(Long expiration) {
                this.expiration = expiration;
            }
        }
    }
}

