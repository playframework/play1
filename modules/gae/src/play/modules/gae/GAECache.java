package play.modules.gae;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import play.Logger;
import play.Play;
import play.cache.Cache.CacheImpl;
import play.exceptions.CacheException;

public class GAECache implements CacheImpl {

    static MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();

    public void add(String key, Object value, int expiration) {
        memcacheService.put(key, wrap(value), Expiration.byDeltaMillis(expiration), MemcacheService.SetPolicy.ADD_ONLY_IF_NOT_PRESENT);
    }

    public boolean safeAdd(String key, Object value, int expiration) {
        memcacheService.put(key, wrap(value), Expiration.byDeltaMillis(expiration), MemcacheService.SetPolicy.ADD_ONLY_IF_NOT_PRESENT);
        return true;
    }

    public void set(String key, Object value, int expiration) {
        memcacheService.put(key, wrap(value), Expiration.byDeltaMillis(expiration), MemcacheService.SetPolicy.SET_ALWAYS);
    }

    public boolean safeSet(String key, Object value, int expiration) {
        memcacheService.put(key, wrap(value), Expiration.byDeltaMillis(expiration), MemcacheService.SetPolicy.SET_ALWAYS);
        return true;
    }

    public void replace(String key, Object value, int expiration) {
        memcacheService.put(key, wrap(value), Expiration.byDeltaMillis(expiration), MemcacheService.SetPolicy.REPLACE_ONLY_IF_PRESENT);
    }

    public boolean safeReplace(String key, Object value, int expiration) {
        memcacheService.put(key, wrap(value), Expiration.byDeltaMillis(expiration), MemcacheService.SetPolicy.REPLACE_ONLY_IF_PRESENT);
        return true;
    }

    public Object get(String key) {
        return unwrap(memcacheService.get(key));
    }

    public Map<String, Object> get(String[] keys) {
        List list = Arrays.asList(keys);
        Map map = memcacheService.getAll(list);
        Map<String,Object> result = new HashMap<String,Object>();
        for(Object key : map.entrySet()) {
            result.put(key.toString(), unwrap(map.get(key)));
        }
        return map;
    }

    public long incr(String key, int by) {
        return memcacheService.increment(key, by);
    }

    public long decr(String key, int by) {
        return memcacheService.increment(key, -by);
    }

    public void clear() {
        memcacheService.clearAll();
    }

    public void delete(String key) {
        memcacheService.delete(key);
    }

    public boolean safeDelete(String key) {
        memcacheService.delete(key);
        return true;
    }

    public void stop() {
    }

    byte[] wrap(Object o) {
        if(o == null) {
            return null;
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bytes);
            oos.writeObject(o);
            return bytes.toByteArray();
        } catch (Exception e) {
            throw new CacheException("Cannot cache a non-serializable value of type " + o.getClass().getName(), e);
        }
    }

    Object unwrap(Object bytes) {
        if(bytes == null) {
            return null;
        }
        try {
            return new ObjectInputStream(new ByteArrayInputStream((byte[])bytes)) {

                @Override
                protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                    return Play.classloader.loadClass(desc.getName());
                }
            }.readObject();
        } catch (Exception e) {
            Logger.error(e, "Error while deserializing cached value");
            return null;
        }
    }
}
