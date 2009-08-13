package unit;

import org.junit.*;
import play.test.*;

import models.*;

public class UserTest extends UnitTest {

    @Before
    public void setUpData() {
        Fixtures.deleteAll();
        Fixtures.load("test-data.yml");
    }

    @Test
    public void countObjects() {
        assertEquals(3, User.count());
    }

    @Test
    public void checkAdmin() {
        User admin = User.findByEmail("admin@sampleforum.com");
        assertNotNull(admin);
        assertEquals("admin@sampleforum.com", admin.email);
        assertEquals("Admin", admin.name);
        assertTrue(admin.checkPassword("hello"));
        assertTrue(admin.isAdmin());
        assertNull(admin.needConfirmation);
    }

    @Test
    public void guillaumeNeedConfirmation() {
        User guillaume = User.findByRegistrationUUID("1234567890");
        assertNotNull(guillaume);
        assertNotNull(guillaume.needConfirmation);
        assertEquals("Guillaume", guillaume.name);
    }

    @Test
    public void createNewUser() {
        assertTrue(User.isEmailAvailable("toto@sampleforum.com"));
        User newUser = new User("toto@sampleforum.com", "hello", "Toto");
        assertEquals("Toto", newUser.name);
        assertEquals("toto@sampleforum.com", newUser.email);
        assertEquals("5d41402abc4b2a76b9719d911017c592", newUser.passwordHash);
        String token = newUser.needConfirmation;
        assertNotNull(token);
        assertEquals(4, User.count());
        User hop = User.findByRegistrationUUID(token);
        assertEquals(newUser, hop);
        assertFalse(User.isEmailAvailable("toto@sampleforum.com"));
    }

    @Test
    public void checkUserPosts() {
        User admin = User.findByEmail("admin@sampleforum.com");
        assertNotNull(admin);
        assertEquals(4L, (long) admin.getPostsCount());
        assertEquals(3L, (long) admin.getTopicsCount());
        assertTrue(admin.getRecentsPosts().get(0).content.contains("Please."));
        assertTrue(admin.getRecentsPosts().get(3).content.contains("It's ok"));
    }

    @Test
    public void freshData() {
        assertEquals(3, User.count());
        assertNull(User.find("byEmail", "toto@sampleforum.com").first());
    }

    @Test
    public void someFinder() {
        assertEquals(0, User.count("byName", "toto"));
        assertEquals(1, User.count("byName", "Guillaume"));
    }
}