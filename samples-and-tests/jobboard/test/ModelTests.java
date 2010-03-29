
import org.junit.*;
import play.test.*;

import models.*;

public class ModelTests extends UnitTest {
    
    @Before
    public void setUpData() {
        Fixtures.deleteAll();
        Fixtures.load("data.yml");
    }
    
    @Test
    public void countObjects() {
        assertEquals(4, Tag.count());
        assertEquals(2, Category.count());
        assertEquals(2, Company.count());
        assertEquals(4, Job.count());
    }
    
    @Test
    public void dynamicallyFindTagsForACategory() {
        assertEquals(2, Tag.findByCategory("business").size());
        assertEquals(3, Tag.findByCategory("development").size());
    }
    
    @Test
    public void findJobsByCategory() {
        assertEquals(3, Job.findByCategoryAndTags(null, null).size());
        assertEquals(1, Job.findByCategoryAndTags("business", null).size());
        assertEquals(2, Job.findByCategoryAndTags("development", null).size());
    }
    
    @Test
    public void findJobsByCategoryAndTags() {
        assertEquals(1, Job.findByCategoryAndTags("business", new String[] {"financial"}).size());
        assertEquals(2, Job.findByCategoryAndTags("development", new String[] {"cool"}).size());
        assertEquals(1, Job.findByCategoryAndTags("development", new String[] {"cool", "java"}).size());
        assertEquals(1, Job.findByCategoryAndTags("development", new String[] {"cool", "php"}).size());
        assertEquals(0, Job.findByCategoryAndTags("development", new String[] {"cool", "java", "php"}).size());
    }
    
    @Test 
    public void likeFinders() {
        Job phpDev = Job.find("byTitleLike", "%php%").first();
        Job javaDev = Job.find("byTitleLike", "%java%").first();
        Job playDev = Job.find("byTitleLike", "%play%").first();
        assertNotNull(phpDev);
        assertNotNull(javaDev);
        assertNotNull(playDev);
    }
    
    @Test
    public void search() {
        assertEquals(1, Job.search("play").size());
        assertEquals(1, Job.search("php").size());
        assertEquals(0, Job.search("java").size()); // offline
        assertEquals(1, Job.search("paris").size());
        assertEquals(2, Job.search("google").size());
        assertEquals(1, Job.search("zenexity").size());
    }
    
}