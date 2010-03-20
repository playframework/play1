package unit;

import org.junit.*;
import play.test.*;

import models.*;

public class PostTest extends UnitTest {

    @Before
    public void setUpData() {
        Fixtures.deleteAll();
        Fixtures.load("test-data.yml");
    }

    @Test
    public void countObjects() {
        assertEquals(7, Post.count());
    }

    @Test
    public void tryAPost() {
        Post post = Post.find("byContent", "Me too.").first();
        assertNotNull(post);
        assertEquals("Jojo", post.postedBy.name);
        assertEquals("I need help !", post.topic.subject);
        assertEquals("Play help", post.topic.forum.name);
    }
}