import org.junit.*;
import play.test.*;
import play.cache.*;
import java.util.*;

import models.*;

public class CacheTest extends UnitTest {
    
    @Before
    public void setup() {
        Fixtures.deleteAll();
        Fixtures.load("users.yml");
        Cache.clear();
    }

    @Test
    public void set() {
        Cache.set("hop", "HOP");
        assertEquals("HOP", Cache.get("hop"));
        assertNull(Cache.get("yep"));
    }
    
    @Test
    public void add() {
        Cache.set("hop", "HOP");
        assertEquals("HOP", Cache.get("hop"));
        assertNull(Cache.get("yep"));
        Cache.add("hop", "HIP");
        Cache.add("yep", "HIP");
        assertEquals("HOP", Cache.get("hop"));
        assertEquals("HIP", Cache.get("yep"));
    }
    
    
    @Test
    public void replace() {
        Cache.set("hop", "HOP");
        assertEquals("HOP", Cache.get("hop"));
        assertNull(Cache.get("yep"));
        Cache.replace("hop", "HIP");
        Cache.replace("yep", "HIP");
        assertEquals("HIP", Cache.get("hop"));
        assertNull(Cache.get("yep"));
    }   
    
    @Test
    public void delete() {
        Cache.set("hop", "HOP");
        Cache.set("hop2", "HOP");
        assertEquals("HOP", Cache.get("hop"));
        assertEquals("HOP", Cache.get("hop2"));
        assertNull(Cache.get("yep"));
        Cache.delete("hop");
        Cache.delete("yep");
        assertEquals("HOP", Cache.get("hop2"));
        assertNull(Cache.get("hop"));
        assertNull(Cache.get("yep"));
    }
    
    @Test
    public void incrDecr() {
        Cache.set("hop", 1L);
        Cache.incr("hop2");
        assertEquals(1L, Cache.get("hop"));
        assertNull(Cache.get("hop2"));
        Cache.incr("hop");
        assertEquals(2L, Cache.get("hop"));
        Cache.incr("hop", 10);
        assertEquals(12L, Cache.get("hop"));
        Cache.decr("hop");
        assertEquals(11L, Cache.get("hop"));
        Cache.decr("hop", 10);
        assertEquals(1L, Cache.get("hop"));
    }
    
    @Test
    public void expiration() {
        Cache.set("hop", "Hop", "2s");
        assertEquals("Hop", Cache.get("hop"));
        pause(2500);
        assertNull(Cache.get("hop"));
        Cache.add("hop", "Hop", "2s");
        assertEquals("Hop", Cache.get("hop"));
        pause(2500);
        assertNull(Cache.get("hop"));
    }
    
    @Test
    public void cacheObjects() {
        List<User> users = User.findAll();
        User u = users.get(0);
        Cache.set("u", u);
        assertEquals(u, Cache.get("u"));
        assertEquals(u, Cache.get("u", User.class));
        Cache.add("users", users);
        assertEquals(users.size(), Cache.get("users", List.class).size());
    }
    
}