import org.junit.*;

import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.test.*;
import models.*;

public class YamlTest extends UnitTest {
        	  
    @BeforeClass
    public static void setUp() {
        Fixtures.deleteDatabase();
        Fixtures.loadModels("yamlTestData.yml");
    }
    
    @Test
    public void testYamlLoading() {
        YamlModel ym = YamlModel.all().first();
        assertEquals("Morten", ym.name);

        assertEquals(DataWithCompositeKey.all().fetch().size(), 2);

        //check binary
        assertEquals("This String is stored in yaml file using base64", new String(ym.binaryData));   
    } 
}
