package unit;

import org.junit.*;
import play.test.*;

import models.*;

public class TopicTest extends UnitTest {

    @Before
    public void setUpData() {
        Fixtures.deleteAll();
        Fixtures.load("test-data.yml");
    }

    @Test
    public void countObjects() {
        assertEquals(4, Topic.count());
    }

    @Test
    public void tryHelpTopic() {
        Topic help = Topic.find("bySubject", "I need help !").first();
        assertNotNull(help);
        assertEquals(3, help.posts.size());
        assertEquals(3L, (long) help.getPostsCount());
        assertEquals(2L, (long) help.getVoicesCount());
        assertEquals("It's ok for me ...", help.getLastPost().content);
        assertEquals("Play help", help.forum.name);
    }

    @Test
    public void newTopic() {
        Forum test = new Forum("Test", "Yop");
        User guillaume = User.find("byName", "Guillaume").first();
        test.newTopic(guillaume, "Hello", "Yop ...");
        assertEquals(2L, (long) guillaume.getTopicsCount());
        assertEquals(1, test.topics.size());
        assertEquals(1, test.getTopicsCount());
        assertEquals(5, Topic.count());
    }

    @Test
    public void testCascadeDelete() {
        Forum help = Forum.find("byName", "Play help").first();
        assertEquals(4, Topic.count());
        assertEquals(7, Post.count());
        help.delete();
        assertEquals(1, Topic.count());
        assertEquals(1, Post.count());
        User guillaume = User.find("byName", "Guillaume").first();
        assertEquals(0L, (long) guillaume.getTopicsCount());
    }
}