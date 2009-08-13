package unit;

import org.junit.*;
import play.test.*;

import models.*;

public class ForumTest extends UnitTest {

    @Before
    public void setUpData() {
        Fixtures.deleteAll();
        Fixtures.load("test-data.yml");
    }

    @Test
    public void countObjects() {
        assertEquals(2, Forum.count());
    }

    @Test
    public void tryHelpForum() {
        Forum help = Forum.find("byName", "Play help").first();
        assertNotNull(help);
        assertEquals(3, help.topics.size());
        assertEquals(3, help.getTopicsCount());
    }

    @Test
    public void tryPagination() {
        Forum help = Forum.find("byName", "Play help").first();
        assertNotNull(help);
        assertEquals(2, help.getTopics(1, 2).size());
        assertEquals(3, help.getTopics(1, 20).size());
        assertEquals(1, help.getTopics(2, 2).size());
        assertEquals(0, help.getTopics(3, 2).size());
        assertEquals(2, help.getTopics(0, 2).size());
        assertEquals("Please help !", help.getTopics(1, 2).get(0).subject);
        assertEquals("It does not work ...", help.getTopics(2, 2).get(0).subject);
    }

    @Test
    public void newForum() {
        Forum test = new Forum("Test", "Yop");
        assertEquals(3, Forum.count());
        assertNotNull(Forum.find("byName", "Test").first());
        assertEquals(test, Forum.find("byName", "Test").first());
    }
}