package play.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.transcoders.SerializingTranscoder;
import play.Logger;
import play.Play;
import play.exceptions.ConfigurationException;
import play.libs.Time;

/**
 * The Cache. Mainly an interface to memcached.
 */
public class Cache {

    private static CacheImpl cacheImpl = new LocalCacheImpl();

    /**
     * Add an element only if it doesn't exist.
     * @param key Element key
     * @param value Element value
     * @param expiration Ex: 10s, 3mn, 8h
     */
    public static void add(String key, Object value, String expiration) {
        cacheImpl.add(key, value, Time.parseDuration(expiration));
    }

    /**
     * Add an element only if it doesn't exist, and return only when 
     * the element is effectivly cached.
     * @param key Element key
     * @param value Element value
     * @param expiration Ex: 10s, 3mn, 8h
     * @return If the element an eventually been cached
     */
    public static boolean safeAdd(String key, Object value, String expiration) {
        return cacheImpl.safeAdd(key, value, Time.parseDuration(expiration));
    }

    /**
     * Add an element only if it doesn't exist and store it indefinitly.
     * @param key Element key
     * @param value Element value
     */
    public static void add(String key, Object value) {
        cacheImpl.add(key, value, Time.parseDuration(null));
    }

    /**
     * Set an element.
     * @param key Element key
     * @param value Element value
     * @param expiration Ex: 10s, 3mn, 8h
     */
    public static void set(String key, Object value, String expiration) {
        cacheImpl.set(key, value, Time.parseDuration(expiration));
    }

    /**
     * Set an element and return only when the element is effectivly cached.
     * @param key Element key
     * @param value Element value
     * @param expiration Ex: 10s, 3mn, 8h
     * @return If the element an eventually been cached
     */
    public static boolean safeSet(String key, Object value, String expiration) {
        return cacheImpl.safeAdd(key, value, Time.parseDuration(expiration));
    }

    /**
     * Set an element and store it indefinitly.
     * @param key Element key
     * @param value Element value
     */
    public static void set(String key, Object value) {
        cacheImpl.set(key, value, Time.parseDuration(null));
    }

    /**
     * Replace an element only if it already exists.
     * @param key Element key
     * @param value Element value
     * @param expiration Ex: 10s, 3mn, 8h
     */
    public static void replace(String key, Object value, String expiration) {
        cacheImpl.replace(key, value, Time.parseDuration(expiration));
    }

    /**
     * Replace an element only if it already exists and return only when the 
     * element is effectivly cached.
     * @param key Element key
     * @param value Element value
     * @param expiration Ex: 10s, 3mn, 8h
     * @return If the element an eventually been cached
     */
    public static boolean safeReplace(String key, Object value, String expiration) {
        return cacheImpl.safeReplace(key, value, Time.parseDuration(expiration));
    }

    /**
     * Replace an element only if it already exists and store it indefinitly.
     * @param key Element key
     * @param value Element value
     */
    public static void replace(String key, Object value) {
        cacheImpl.replace(key, value, Time.parseDuration(null));
    }

    /**
     * Increment the element value (must be a Number).
     * @param key Element key 
     * @param by The incr value
     * @return The new value
     */
    public static long incr(String key, int by) {
        return cacheImpl.incr(key, by);
    }

    /**
     * Increment the element value (must be a Number) by 1.
     * @param key Element key 
     * @return The new value
     */
    public static long incr(String key) {
        return cacheImpl.incr(key, 1);
    }

    /**
     * Decrement the element value (must be a Number).
     * @param key Element key 
     * @param by The decr value
     * @return The new value
     */
    public static long decr(String key, int by) {
        return cacheImpl.decr(key, by);
    }

    /**
     * Decrement the element value (must be a Number) by 1.
     * @param key Element key 
     * @return The new value
     */
    public static long decr(String key) {
        return cacheImpl.decr(key, 1);
    }

    /**
     * Retrieve an object.
     * @param key The element key
     * @return The element value or null
     */
    public static Object get(String key) {
        return cacheImpl.get(key);
    }

    /**
     * Bulk retrieve.
     * @param key List of keys
     * @return Map of keys & values
     */
    public static Map<String, Object> get(String... key) {
        return cacheImpl.get(key);
    }

    /**
     * Delete an element from the cache.
     * @param key The element key     * 
     */
    public static void delete(String key) {
        cacheImpl.delete(key);
    }

    /**
     * Delete an element from the cache and return only when the 
     * element is effectivly removed.
     * @param key The element key
     * @return If the element an eventually been deleted
     */
    public static boolean safeDelete(String key) {
        return cacheImpl.safeDelete(key);
    }

    /**
     * Clear all data from cache.
     */
    public static void clear() {
        cacheImpl.clear();
    }

    /**
     * Convenient clazz to get a value a class type;
     * @param <T> The needed type
     * @param key The element key
     * @param clazz The type class
     * @return The element value or null
     */
    public static <T> T get(String key, Class<T> clazz) {
        return (T) cacheImpl.get(key);
    }

    /**
     * Init the cache system.
     */
    public static void init() {
        if (Play.configuration.getProperty("memcached", "disabled").equals("enabled")) {
            try {
                cacheImpl = new MemcachedImpl();
                Logger.info("Connected to memcached");
            } catch (Exception e) {
                Logger.error(e, "Error while connecting to memcached");
                Logger.warn("Fallback to local cache");
                cacheImpl = new LocalCacheImpl();
            }
        } else {
            cacheImpl = new LocalCacheImpl();
        }
    }

    /**
     * Stop the cache system.
     */
    public static void stop() {
        cacheImpl.stop();
    }

    /**
     * A cache implementation
     */
    static interface CacheImpl {

        public void add(String key, Object value, int expiration);

        public boolean safeAdd(String key, Object value, int expiration);

        public void set(String key, Object value, int expiration);

        public boolean safeSet(String key, Object value, int expiration);

        public void replace(String key, Object value, int expiration);

        public boolean safeReplace(String key, Object value, int expiration);

        public Object get(String key);

        public Map<String, Object> get(String[] keys);

        public long incr(String key, int by);

        public long decr(String key, int by);

        public void clear();

        public void delete(String key);

        public boolean safeDelete(String key);

        public void stop();
    }

    /**
     * Memcached implementation
     */
    static class MemcachedImpl implements CacheImpl {

        MemcachedClient client;

        public MemcachedImpl() throws IOException {
            System.setProperty("net.spy.log.LoggerImpl", "net.spy.log.Log4JLogger");
            if (Play.configuration.containsKey("memcached.host")) {
                client = new MemcachedClient(AddrUtil.getAddresses(Play.configuration.getProperty("memcached.host")));
                client.setTranscoder(new SerializingTranscoder() {

                    @Override
                    protected Object deserialize(byte[] data) {
                        try {
                            return new ObjectInputStream(new ByteArrayInputStream(data)) {

                                @Override
                                protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                                    return Play.classloader.loadClass(desc.getName());
                                }
                            }.readObject();
                        } catch (Exception e) {
                            Logger.error(e, "Could not deserialize");
                        }
                        return null;
                    }

                    @Override
                    protected byte[] serialize(Object object) {
                        try {
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            new ObjectOutputStream(bos).writeObject(object);
                            return bos.toByteArray();
                        } catch (IOException e) {
                            Logger.error(e, "Could not serialize");
                        }
                        return null;
                    }
                });
            } else if (Play.configuration.containsKey("memcached.1.host")) {
                int nb = 1;
                String addresses = "";
                while (Play.configuration.containsKey("memcached." + nb + ".host")) {
                    addresses += Play.configuration.get("memcached." + nb + ".host") + " ";
                    nb++;
                }
                client = new MemcachedClient(AddrUtil.getAddresses(addresses));
            } else {
                throw new ConfigurationException(("Bad configuration for memcached"));
            }
        }

        public void add(String key, Object value, int expiration) {
            client.add(key, expiration, value);
        }

        public Object get(String key) {
            Future<Object> future = client.asyncGet(key);
            try {
                return future.get(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                future.cancel(false);
            }
            return null;
        }

        public void clear() {
            client.flush();
        }

        public void delete(String key) {
            client.delete(key);
        }

        public Map<String, Object> get(String[] keys) {
            Future<Map<String, Object>> future = client.asyncGetBulk(keys);
            try {
                return future.get(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                future.cancel(false);
            }
            return new HashMap<String, Object>();
        }

        public long incr(String key, int by) {
            return client.incr(key, by);
        }

        public long decr(String key, int by) {
            return client.decr(key, by);
        }

        public void replace(String key, Object value, int expiration) {
            client.replace(key, expiration, value);
        }

        public boolean safeAdd(String key, Object value, int expiration) {
            Future<Boolean> future = client.add(key, expiration, value);
            try {
                return future.get(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                future.cancel(false);
            }
            return false;
        }

        public boolean safeDelete(String key) {
            Future<Boolean> future = client.delete(key);
            try {
                return future.get(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                future.cancel(false);
            }
            return false;
        }

        public boolean safeReplace(String key, Object value, int expiration) {
            Future<Boolean> future = client.replace(key, expiration, value);
            try {
                return future.get(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                future.cancel(false);
            }
            return false;
        }

        public boolean safeSet(String key, Object value, int expiration) {
            Future<Boolean> future = client.set(key, expiration, value);
            try {
                return future.get(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                future.cancel(false);
            }
            return false;
        }

        public void set(String key, Object value, int expiration) {
            client.set(key, expiration, value);
        }

        public void stop() {
            client.shutdown();
        }
    }

    /**
     * In JVM heap implementation
     */
    static class LocalCacheImpl implements CacheImpl {

        private Map<String, CachedElement> cache = new HashMap<String, CachedElement>();

        public void add(String key, Object value, int expiration) {
            safeAdd(key, value, expiration);
        }

        public Object get(String key) {
            CachedElement cachedElement = cache.get(key);
            if (cachedElement != null && System.currentTimeMillis() >= cachedElement.getExpiration()) {
                cache.remove(key);
                return null;
            }
            return cachedElement == null ? null : cachedElement.getValue();
        }

        public void delete(String key) {
            safeDelete(key);
        }

        public Map<String, Object> get(String[] keys) {
            Map<String, Object> result = new HashMap<String, Object>();
            for (String key : keys) {
                result.put(key, get(key));
            }
            return result;
        }

        public synchronized long incr(String key, int by) {
            CachedElement cachedElement = cache.get(key);
            if (cachedElement == null) {
                return -1;
            }
            long newValue = (Long) cachedElement.getValue() + by;
            cachedElement.setValue(newValue);
            return newValue;
        }

        public synchronized long decr(String key, int by) {
            CachedElement cachedElement = cache.get(key);
            if (cachedElement == null) {
                return -1;
            }
            long newValue = (Long) cachedElement.getValue() - by;
            cachedElement.setValue(newValue);
            return newValue;
        }

        public void replace(String key, Object value, int expiration) {
            safeReplace(key, value, expiration);
        }

        public void set(String key, Object value, int expiration) {
            safeSet(key, value, expiration);
        }

        public boolean safeAdd(String key, Object value, int expiration) {
            Object v = get(key);
            if (v == null) {
                set(key, value, expiration);
                return true;
            }
            return false;
        }

        public boolean safeDelete(String key) {
            CachedElement cachedElement = cache.get(key);
            if (cachedElement != null) {
                cache.remove(key);
                return true;
            }
            return false;
        }

        public boolean safeReplace(String key, Object value, int expiration) {
            CachedElement cachedElement = cache.get(key);
            if (cachedElement == null) {
                return false;
            }
            cachedElement.setExpiration(expiration * 1000 + System.currentTimeMillis());
            cachedElement.setValue(value);
            return true;
        }

        public boolean safeSet(String key, Object value, int expiration) {
            cache.put(key, new CachedElement(key, value, expiration * 1000 + System.currentTimeMillis()));
            return true;
        }

        public void stop() {
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

