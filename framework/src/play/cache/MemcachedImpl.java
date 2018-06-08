package play.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;
import net.spy.memcached.transcoders.SerializingTranscoder;
import play.Logger;
import play.Play;
import play.exceptions.ConfigurationException;

/**
 * Memcached implementation (using http://code.google.com/p/spymemcached/)
 *
 * expiration is specified in seconds
 */
public class MemcachedImpl implements CacheImpl {

    private static MemcachedImpl uniqueInstance;

    MemcachedClient client;

    SerializingTranscoder tc;

    public static MemcachedImpl getInstance() throws IOException {
      return getInstance(false);
    }

    public static MemcachedImpl getInstance(boolean forceClientInit) throws IOException {
        if (uniqueInstance == null) {
            uniqueInstance = new MemcachedImpl();
        } else if (forceClientInit) {
            // When you stop the client, it sets the interrupted state of this thread to true. If you try to reinit it with the same thread in this state,
            // Memcached client errors out. So a simple call to interrupted() will reset this flag
            Thread.interrupted();
            uniqueInstance.initClient();
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
        initClient();
    }

    public void initClient() throws IOException {
        System.setProperty("net.spy.log.LoggerImpl", "net.spy.memcached.compat.log.Log4JLogger");
        
        List<InetSocketAddress> addrs;
        if (Play.configuration.containsKey("memcached.host")) {
            addrs = AddrUtil.getAddresses(Play.configuration.getProperty("memcached.host"));
        } else if (Play.configuration.containsKey("memcached.1.host")) {
            int nb = 1;
            String addresses = "";
            while (Play.configuration.containsKey("memcached." + nb + ".host")) {
                addresses += Play.configuration.get("memcached." + nb + ".host") + " ";
                nb++;
            }
            addrs = AddrUtil.getAddresses(addresses);
        } else {
            throw new ConfigurationException("Bad configuration for memcached: missing host(s)");
        }
        
        if (Play.configuration.containsKey("memcached.user")) {
            String memcacheUser = Play.configuration.getProperty("memcached.user");
            String memcachePassword = Play.configuration.getProperty("memcached.password");
            if (memcachePassword == null) {
                throw new ConfigurationException("Bad configuration for memcached: missing password");
            }
            
            // Use plain SASL to connect to memcached
            AuthDescriptor ad = new AuthDescriptor(new String[]{"PLAIN"},
                                    new PlainCallbackHandler(memcacheUser, memcachePassword));
            ConnectionFactory cf = new ConnectionFactoryBuilder()
                                        .setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
                                        .setAuthDescriptor(ad)
                                        .build();
            
            client = new MemcachedClient(cf, addrs);
        } else {
            client = new MemcachedClient(addrs);
        }
    }

    @Override
    public void add(String key, Object value, int expiration) {
        client.add(key, expiration, value, tc);
    }

    @Override
    public Object get(String key) {
        Future<Object> future = client.asyncGet(key, tc);
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.cancel(false);
        }
        return null;
    }

    @Override
    public void clear() {
        client.flush();
    }

    @Override
    public void delete(String key) {
        client.delete(key);
    }

    @Override
    public Map<String, Object> get(String[] keys) {
        Future<Map<String, Object>> future = client.asyncGetBulk(tc, keys);
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.cancel(false);
        }
        return Collections.<String, Object>emptyMap();
    }

    @Override
    public long incr(String key, int by) {
        return client.incr(key, by, 0);
    }

    @Override
    public long decr(String key, int by) {
        return client.decr(key, by, 0);
    }

    @Override
    public void replace(String key, Object value, int expiration) {
        client.replace(key, expiration, value, tc);
    }

    @Override
    public boolean safeAdd(String key, Object value, int expiration) {
        Future<Boolean> future = client.add(key, expiration, value, tc);
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.cancel(false);
        }
        return false;
    }

    @Override
    public boolean safeDelete(String key) {
        Future<Boolean> future = client.delete(key);
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.cancel(false);
        }
        return false;
    }

    @Override
    public boolean safeReplace(String key, Object value, int expiration) {
        Future<Boolean> future = client.replace(key, expiration, value, tc);
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.cancel(false);
        }
        return false;
    }

    @Override
    public boolean safeSet(String key, Object value, int expiration) {
        Future<Boolean> future = client.set(key, expiration, value, tc);
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.cancel(false);
        }
        return false;
    }

    @Override
    public void set(String key, Object value, int expiration) {
        client.set(key, expiration, value, tc);
    }

    @Override
    public void stop() {
        client.shutdown();
    }
}
