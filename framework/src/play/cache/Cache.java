package play.cache;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import play.Logger;
import play.Play;
import play.exceptions.ConfigurationException;
import play.libs.Time;

public class Cache {

    private static CacheImpl cacheImpl = new LocalCacheImpl();

    public static void add(String key, Object value, String expiration) {
        cacheImpl.add(key, value, Time.parseDuration(expiration));
    }
    
    public static boolean safeAdd(String key, Object value, String expiration) {
        return cacheImpl.safeAdd(key, value, Time.parseDuration(expiration));
    }

    public static void add(String key, Object value) {
        cacheImpl.add(key, value, Time.parseDuration(null));
    }
    
    public static boolean safeAdd(String key, Object value) {
        return cacheImpl.safeAdd(key, value, Time.parseDuration(null));
    }
    
    public static void set(String key, Object value, String expiration) {
        cacheImpl.set(key, value, Time.parseDuration(expiration));
    }
    
    public static boolean safeSet(String key, Object value, String expiration) {
        return cacheImpl.safeAdd(key, value, Time.parseDuration(expiration));
    }

    public static void set(String key, Object value) {
        cacheImpl.set(key, value, Time.parseDuration(null));
    }
    
    public static boolean safeSet(String key, Object value) {
        return cacheImpl.safeSet(key, value, Time.parseDuration(null));
    }
    
    public static void replace(String key, Object value, String expiration) {
        cacheImpl.replace(key, value, Time.parseDuration(expiration));
    }
    
    public static boolean safeReplace(String key, Object value, String expiration) {
        return cacheImpl.safeReplace(key, value, Time.parseDuration(expiration));
    }

    public static void replace(String key, Object value) {
        cacheImpl.replace(key, value, Time.parseDuration(null));
    }
    
    public static boolean safeReplace(String key, Object value) {
        return cacheImpl.safeReplace(key, value, Time.parseDuration(null));
    }
    
    public static long incr(String key, int by) {
        return cacheImpl.incr(key, by);
    }
    
    public static long incr(String key) {
        return cacheImpl.incr(key, 1);
    }
    
    public static long decr(String key, int by) {
        return cacheImpl.decr(key, by);
    }
    
    public static long decr(String key) {
        return cacheImpl.decr(key, 1);
    }

    public static Object get(String key) {
        return cacheImpl.get(key);
    }
    
    public static Map<String,Object> get(String... key) {
        return cacheImpl.get(key);
    }

    public static void delete(String key) {
        cacheImpl.delete(key);
    }
    
    public static boolean safeDelete(String key) {
        return cacheImpl.safeDelete(key);
    }

    public static void clear() {
        cacheImpl.clear();
    }

    public static <T> T get(String key, Class<T> clazz) {
        return (T) cacheImpl.get(key);
    }
    
    public static void init() {
        if(Play.configuration.getProperty("memcached", "disabled").equals("enabled")) {
            try {
                cacheImpl = new MemcachedImpl();
            } catch(Exception e) {
                Logger.error(e, "Error while connecting to memcached");
                Logger.warn("Fallback to local cache");
                cacheImpl = new LocalCacheImpl();
            }
        } else {
            cacheImpl = new LocalCacheImpl();
        }
    }
    
    public static void stop() {
        cacheImpl.stop();
    }
    
    static interface CacheImpl {
        public void add(String key, Object value, int expiration);
        public boolean safeAdd(String key, Object value, int expiration);
        public void set(String key, Object value, int expiration);
        public boolean safeSet(String key, Object value, int expiration);
        public void replace(String key, Object value, int expiration);
        public boolean safeReplace(String key, Object value, int expiration);
        public Object get(String key);
        public Map<String,Object> get(String[] keys);
        public long incr(String key, int by); 
        public long decr(String key, int by); 
        public void clear();
        public void delete(String key);
        public boolean safeDelete(String key);
        public void stop();
    }
    
    static class MemcachedImpl implements CacheImpl {
        
        MemcachedClient client;
        
        public MemcachedImpl() throws IOException {
            System.setProperty("net.spy.log.LoggerImpl", "net.spy.log.Log4JLogger");
            if(Play.configuration.containsKey("memcached.host")) {
                client = new MemcachedClient(AddrUtil.getAddresses(Play.configuration.getProperty("memcached.host")));                
            } else if(Play.configuration.containsKey("memcached.1.host")) {   
                int nb = 1;
                String addresses = "";
                while(Play.configuration.containsKey("memcached."+nb+".host")) {
                    addresses += Play.configuration.get("memcached."+nb+".host") + " ";
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
            } catch(Exception e) {
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
            } catch(Exception e) {
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
            } catch(Exception e) {
                future.cancel(false);
            }
            return false;
        }

        public boolean safeDelete(String key) {
            Future<Boolean> future = client.delete(key);
            try {
                return future.get(1, TimeUnit.SECONDS);
            } catch(Exception e) {
                future.cancel(false);
            }
            return false;
        }

        public boolean safeReplace(String key, Object value, int expiration) {
            Future<Boolean> future = client.replace(key, expiration, value);
            try {
                return future.get(1, TimeUnit.SECONDS);
            } catch(Exception e) {
                future.cancel(false);
            }
            return false;
        }

        public boolean safeSet(String key, Object value, int expiration) {
            Future<Boolean> future = client.set(key, expiration, value);
            try {
                return future.get(1, TimeUnit.SECONDS);
            } catch(Exception e) {
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
            for(String key : keys) {
                result.put(key, get(key));
            }
            return result;
        }

        public synchronized long incr(String key, int by) {
            CachedElement cachedElement = cache.get(key);
            if(cachedElement == null) {
                return -1;
            }
            long newValue = (Long)cachedElement.getValue() + by;
            cachedElement.setValue(newValue);
            return newValue;
        }

        public synchronized long decr(String key, int by) {
            CachedElement cachedElement = cache.get(key);
            if(cachedElement == null) {
                return -1;
            }
            long newValue = (Long)cachedElement.getValue() - by;
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
            if(cachedElement == null) {
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

