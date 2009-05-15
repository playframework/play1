
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
        Topic help = Topic.find("bySubject", "I need help !").one();
        assertNotNull(help);
        assertEquals(3, help.posts.size());
        assertEquals(3L, (long)help.getPostsCount());
        assertEquals(2L, (long)help.getVoicesCount());
        assertEquals("It's ok for me ...", help.getLastPost().content);
        assertEquals("Play help", help.forum.name);
    }   
    
    
}