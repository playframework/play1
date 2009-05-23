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
        assertEquals(109, a.birth.getYear());
        assertEquals(11, a.birth.getMonth());
        assertEquals(12, a.birth.getDate());
    }
    
    @Test
    public void dynamicFinders() {
        List<User> users = User.findAll();
        User a = users.get(0);
        User b = users.get(1);
        //
        assertEquals(a, User.find("byName", "A").one());
        assertEquals(b, User.find("byNameAndC", "B", true).one());
        assertNull(User.find("byNameAndC", "B", false).one());
        assertEquals(a, User.find("byNameLikeAndJ", "%a%", 45).one());
        assertEquals(b, User.find("byNameLikeAndBAndCAndLAndI", "%b%", false, true, 10000L, 34).one());
        assertNull(User.find("byNameLikeAndBAndCAndLAndI", "%b%", false, true, 10000L, 32).one());
        assertEquals(a, User.find("byBIsNull").one());
        assertEquals(b, User.find("byBIsNotNull").one());
    }
    
    @Test
    public void simpleFinders() {
        List<User> users = User.findAll();
        User a = users.get(0);
        User b = users.get(1);
        //
        assertEquals(a, User.find("name", "A").one());
        assertEquals(a, User.find("name = ?", "A").one());
        assertEquals(a, User.find("name=?", "A").one());
        assertEquals(b, User.find("name = ? and c = ?", "B", true).one());
        assertNull(User.find("name = ? and c = ?", "B", false).one());
        assertEquals(a, User.find("name like ? and j = ?", "%A%", 45).one());
        assertEquals(b, User.find("name like ? and b = ? and c = ? and l = ? and i = ?", "%B%", false, true, 10000L, 34).one());
        assertNull(User.find("name like ? and b = ? and c = ? and l = ? and i = ?", "%B%", false, true, 10000L, 32).one());
        assertEquals(a, User.find("b is null").one());
        assertEquals(b, User.find("b is not null").one());
    }  
    
    @Test
    public void fullFinders() {
        List<User> users = User.findAll();
        User a = users.get(0);
        User b = users.get(1);
        //
        assertEquals(a, User.find("from User where name = ?", "A").one());
        assertEquals(b, User.find("from User where  name = ? and c = ?", "B", true).one());
        assertNull(User.find("from User where name = ? and c = ?", "B", false).one());
        assertEquals(a, User.find("from User where name like ? and j = ?", "%A%", 45).one());
        assertEquals(b, User.find("from User where name like ? and b = ? and c = ? and l = ? and i = ?", "%B%", false, true, 10000L, 34).one());
        assertNull(User.find("from User where name like ? and b = ? and c = ? and l = ? and i = ?", "%B%", false, true, 10000L, 32).one());
        assertEquals(a, User.find("from User where b is null").one());
        assertEquals(b, User.find("from User where b is not null").one());
        
        assertEquals(a, User.find("select u from User u where u.name = ?", "A").one());
        assertEquals(b, User.find("select u from User u where u.name = ? and u.c = ?", "B", true).one());
        assertNull(User.find("select u from User u where u.name = ? and u.c = ?", "B", false).one());
        assertEquals(a, User.find("select u from User u where u.name like ? and u.j = ?", "%A%", 45).one());
        assertEquals(b, User.find("select u from User u where u.name like ? and u.b = ? and u.c = ? and u.l = ? and u.i = ?", "%B%", false, true, 10000L, 34).one());
        assertNull(User.find("select u from User u where u.name like ? and u.b = ? and u.c = ? and u.l = ? and u.i = ?", "%B%", false, true, 10000L, 32).one());
        assertEquals(a, User.find("select u from User u where u.b is null").one());
        assertEquals(b, User.find("select u from User u where u.b is not null").one());
    } 
    
    @Test
    public void orderBy() { 
        List<User> users = User.findAll();
        User a = users.get(0);
        User b = users.get(1);
        //
        assertEquals(a, User.find("from User order by name ASC").one());
        assertEquals(b, User.find("from User order by name DESC").one()); 
        assertEquals(a, User.find("from User order by b ASC").one());
        assertEquals(b, User.find("from User order by b DESC").one());
        assertEquals(b, User.find("from User order by j ASC").one());
        assertEquals(a, User.find("from User order by j DESC").one());
    }
    
}

