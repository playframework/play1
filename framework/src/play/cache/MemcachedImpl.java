package play.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.transcoders.SerializingTranscoder;
import play.Logger;
import play.Play;
import play.exceptions.ConfigurationException;

/**
 * Memcached implementation (using http://code.google.com/p/spymemcached/)
 */
public class MemcachedImpl implements CacheImpl {

    private static MemcachedImpl uniqueInstance;

    MemcachedClient client;

    SerializingTranscoder tc;

    public static MemcachedImpl getInstance() throws IOException {
        if (uniqueInstance == null) {
            uniqueInstance = new MemcachedImpl();
        }
        return uniqueInstance;
    }

    private MemcachedImpl() throws IOException {
        tc = new SerializingTranscoder() {

            @Override
            protected Object deserialize(byte[] data) {
                try {
                    return new ObjectInputStream(new ByteArrayInputStream(data)) {

                        @Override
                        protected Class<?> resolveClass(ObjectStreamClass desc)
                                throws IOException, ClassNotFoundException {
                            return Class.forName(desc.getName(), false, Play.classloader);
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
        };

        System.setProperty("net.spy.log.LoggerImpl", "net.spy.memcached.compat.log.Log4JLogger");
        if (Play.configuration.containsKey("memcached.host")) {
            client = new MemcachedClient(AddrUtil.getAddresses(Play.configuration.getProperty("memcached.host")));
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
        client.add(key, expiration, value, tc);
    }

    public Object get(String key) {
        Future<Object> future = client.asyncGet(key, tc);
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
        Future<Map<String, Object>> future = client.asyncGetBulk(tc, keys);
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.cancel(false);
        }
        return Collections.<String, Object>emptyMap();
    }

    public long incr(String key, int by) {
        return client.incr(key, by);
    }

    public long decr(String key, int by) {
        return client.decr(key, by);
    }

    public void replace(String key, Object value, int expiration) {
        client.replace(key, expiration, value, tc);
    }

    public boolean safeAdd(String key, Object value, int expiration) {
        Future<Boolean> future = client.add(key, expiration, value, tc);
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
        Future<Boolean> future = client.replace(key, expiration, value, tc);
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.cancel(false);
        }
        return false;
    }

    public boolean safeSet(String key, Object value, int expiration) {
        Future<Boolean> future = client.set(key, expiration, value, tc);
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.cancel(false);
        }
        return false;
    }

    public void set(String key, Object value, int expiration) {
        client.set(key, expiration, value, tc);
    }

    public void stop() {
        client.shutdown();
    }
}
