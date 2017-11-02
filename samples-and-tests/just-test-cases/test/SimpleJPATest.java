import org.junit.*;
import play.test.*;
import java.util.*;

import models.*;

public class SimpleJPATest extends UnitTest {

    @Before
    public void setup() {
        Fixtures.deleteAll();
        Fixtures.load("users.yml");
    }

    @Test
    public void testImport() {
        List<User> f = User.find("byNameLike", "%").fetch();
        assertEquals(2, f.size());
        assertEquals(2, User.count());
        List<User> users = User.findAll();
        User a = users.get(0);
        User b = users.get(1);
        assertNotNull(a);
        assertNotNull(b);
        assertEquals("A", a.name);
        assertEquals("B", b.name);
        assertFalse(a.c);
        assertTrue(b.c);
        assertNull(a.b);
        assertFalse(b.b);
        assertNull(a.i);
        assertEquals(34, (int)b.i);
        assertEquals(45, a.j);
        assertEquals(0, b.j);
        assertEquals(34, (int)b.i);
        assertEquals(10000, b.l);
        assertEquals(0, a.l);
        assertNull(a.k);
        assertNull(b.birth);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(a.birth);
        assertEquals(2009, cal.get(Calendar.YEAR));
        assertEquals(11, cal.get(Calendar.MONTH));
        assertEquals(12, cal.get(Calendar.DAY_OF_MONTH));
    }
    
    @Test
    public void dynamicFinders() {
        List<User> users = User.findAll();
        User a = users.get(0);
        User b = users.get(1);
        //
        assertEquals(a, User.find("byName", "A").first());
        assertEquals(b, User.find("byNameAndC", "B", true).first());
        assertNull(User.find("byNameAndC", "B", false).first());
        assertEquals(a, User.find("byNameLikeAndJ", "%a%", 45).first());
        assertEquals(b, User.find("byNameLikeAndBAndCAndLAndI", "%b%", false, true, 10000L, 34).first());
        assertNull(User.find("byNameLikeAndBAndCAndLAndI", "%b%", false, true, 10000L, 32).first());
        assertEquals(a, User.find("byBIsNull").first());
        assertEquals(b, User.find("byBIsNotNull").first());
        
        List<User> usersFounded;
        //Elike
        usersFounded = User.find("byNameElikeAndJ", "%a%", 45).fetch();
        assertEquals(0, usersFounded.size());
        usersFounded = User.find("byNameElikeAndJ", "%A%", 45).fetch();
        assertEquals(1, usersFounded.size());
        //Ilike
        usersFounded = User.find("byNameIlikeAndJ", "%A%", 45).fetch();
        assertEquals(1, usersFounded.size());
        assertEquals(a, usersFounded.get(0));
        //NotEqual
        usersFounded = User.find("byJNotEqual", 45).fetch();
        assertEquals(1, usersFounded.size());
        assertEquals(b, usersFounded.get(0));
        //LessThan
        usersFounded = User.find("byILessThan", 34).fetch();
        assertEquals(0, usersFounded.size());
        usersFounded = User.find("byILessThan", 35).fetch();
        assertEquals(b, usersFounded.get(0));
        //LessThanEquals
        usersFounded = User.find("byILessThanEquals", 33).fetch();
        assertEquals(0, usersFounded.size());
        usersFounded = User.find("byILessThanEquals", 34).fetch();
        assertEquals(b, usersFounded.get(0));
        //GreaterThan
        usersFounded = User.find("byIGreaterThan", 34).fetch();
        assertEquals(0, usersFounded.size());
        usersFounded = User.find("byIGreaterThan", 33).fetch();
        assertEquals(b, usersFounded.get(0));
        //GreaterThanEquals
        usersFounded = User.find("byIGreaterThanEquals", 35).fetch();
        assertEquals(0, usersFounded.size());
        usersFounded = User.find("byIGreaterThanEquals", 34).fetch();
        assertEquals(b, usersFounded.get(0));
        
    }
    
    @Test
    public void simpleFinders() {
        List<User> users = User.findAll();
        User a = users.get(0);
        User b = users.get(1);
        //
        assertEquals(a, User.find("name", "A").first());
        assertEquals(a, User.find("name = ?1", "A").first());
        assertEquals(a, User.find("name=?1", "A").first());
        assertEquals(b, User.find("name = ?1 and c = ?2", "B", true).first());
        assertNull(User.find("name = ?1 and c = ?2", "B", false).first());
        assertEquals(a, User.find("name like ?1 and j = ?2", "%A%", 45).first());
        assertEquals(b, User.find("name like ?1 and b = ?2 and c = ?3 and l = ?4 and i = ?5", "%B%", false, true, 10000L, 34).first());
        assertNull(User.find("name like ?1 and b = ?2 and c = ?3 and l = ?4 and i = ?5", "%B%", false, true, 10000L, 32).first());
        assertEquals(a, User.find("b is null").first());
        assertEquals(b, User.find("b is not null").first());
    }  
    
    @Test
    public void fullFinders() {
        List<User> users = User.findAll();
        User a = users.get(0);
        User b = users.get(1);
        //
        assertEquals(a, User.find("from User where name = ?1", "A").first());
        assertEquals(b, User.find("from User where  name = ?1 and c = ?2", "B", true).first());
        assertNull(User.find("from User where name = ?1 and c = ?2", "B", false).first());
        assertEquals(a, User.find("from User where name like ?1 and j = ?2", "%A%", 45).first());
        assertEquals(b, User.find("from User where name like ?1 and b = ?2 and c = ?3 and l = ?4 and i = ?5", "%B%", false, true, 10000L, 34).first());
        assertNull(User.find("from User where name like ?1 and b = ?2 and c = ?3 and l = ?4 and i = ?5", "%B%", false, true, 10000L, 32).first());
        assertEquals(a, User.find("from User where b is null").first());
        assertEquals(b, User.find("from User where b is not null").first());
        
        assertEquals(a, User.find("select u from User u where u.name = ?1", "A").first());
        assertEquals(b, User.find("select u from User u where u.name = ?1 and u.c = ?2", "B", true).first());
        assertNull(User.find("select u from User u where u.name = ?1 and u.c = ?2", "B", false).first());
        assertEquals(a, User.find("select u from User u where u.name like ?1 and u.j = ?2", "%A%", 45).first());
        assertEquals(b, User.find("select u from User u where u.name like ?1 and u.b = ?2 and u.c = ?3 and u.l = ?4 and u.i = ?5", "%B%", false, true, 10000L, 34).first());
        assertNull(User.find("select u from User u where u.name like ?1 and u.b = ?2 and u.c = ?3 and u.l = ?4 and u.i = ?5", "%B%", false, true, 10000L, 32).first());
        assertEquals(a, User.find("select u from User u where u.b is null").first());
        assertEquals(b, User.find("select u from User u where u.b is not null").first());
    } 
    
    @Test
    public void orderBy() { 
        List<User> users = User.findAll();
        User a = users.get(0);
        User b = users.get(1);
        //
        assertEquals(a, User.find("from User order by name ASC").first());
        assertEquals(b, User.find("from User order by name DESC").first()); 
        assertEquals(a, User.find("from User order by b ASC").first());
        assertEquals(b, User.find("from User order by b DESC").first());
        assertEquals(b, User.find("from User order by j ASC").first());
        assertEquals(a, User.find("from User order by j DESC").first());
    }
    
    @Test
    public void verifyCountWithCompositeKey() {
        
        List<DataWithCompositeKey> list = DataWithCompositeKey.findAll();
        for (DataWithCompositeKey d : list) {
            d.delete();
        }
        DataWithCompositeKey d = new DataWithCompositeKey();
        d.key1 = "1";
        d.key2 = "1";
        d.save();
        
        d = new DataWithCompositeKey();
        d.key1 = "1";
        d.key2 = "2";
        d.save();
        
        assertEquals(2l, DataWithCompositeKey.count());
        assertEquals(2l, DataWithCompositeKey.count(""));

        d =  DataWithCompositeKey.findById(new DataWithCompositeKey("1", "2"));
        assertEquals("1", d.key1);
        assertEquals("2", d.key2);

    }
    
    /**
     * Simple tests for {@link play.db.jpa.JPABase#equals()}.
     */
    @Test
    public void testEquals() {
        List<User> users = User.findAll();
        User userA = users.get(0);
        User userB = users.get(1);
        
        // Simple tests
        assertFalse(userA.equals(null)); // null
        assertTrue(userA.equals(userA)); // ref equals
        assertFalse(userA.equals(userB)); // different objects
        
        // tests with objects that don't have a key set
        User unsaved1 = new User("Unsaved user #1");
        User unsaved2 = new User("Unsaved user #2");
        assertTrue(unsaved1.equals(unsaved1)); // ref equals (always true)
        assertFalse(unsaved1.equals(unsaved2)); // non-ref+no key
        assertFalse(unsaved1.equals(userA));
        assertFalse(userA.equals(unsaved1));
        
        // Test completely incompatible objects
        assertFalse(userA.equals("This is a string, not a model object."));

        // Test with array IDs
        ArrayIdEntity a1 = new ArrayIdEntity("1", "2");
        ArrayIdEntity a2 = new ArrayIdEntity("1", "2");
        ArrayIdEntity b1 = new ArrayIdEntity("1", "X");
        
        assertTrue(a1.equals(a2)); // array key equals
        assertFalse(a1.equals(b1)); // array key with one element difference
        assertFalse(userA.equals(a1)); // compare scalar key to array key
        assertFalse(a1.equals(userA)); // compare array key with scalar key
    }
}