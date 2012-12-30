import org.junit.*;

import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.test.*;
import models.*;

public class YamlTest extends UnitTest {
    
    public static boolean isJpaForced = false;
    
    public static void checkJpaOpen() {
	    if (JPA.local.get() == null) {
	      isJpaForced = true;
	    }
	    if (isJpaForced == true) {
	      // workaround (Start Junit Test) bug
	      // https://bugs.launchpad.net/play/+bug/491403
	      JPAPlugin.startTx(false);
	    }
	  }

	  public static void checkJpaClose() {
	    if (isJpaForced == true) {
	      // workaround (Start Junit Test) bug
	      // https://bugs.launchpad.net/play/+bug/491403
	      JPAPlugin.closeTx(false);
	      isJpaForced = false;
	    }
	  }
	  
    @BeforeClass
    public static void setUp() {
	checkJpaOpen();
	Fixtures.deleteAll();
        Fixtures.load("yamlTestData.yml");
        checkJpaClose();
    }
    
    @Test
    public void testYamlLoading_1() {
        YamlModel ym = YamlModel.all().first();
        assertEquals("Morten", ym.name);

        assertEquals(DataWithCompositeKey.all().fetch().size(), 2);

        //check binary
        assertEquals("This String is stored in yaml file using base64", new String(ym.binaryData));
        
    }
    
    @Test
    public void testYamlLoading_2() {
        Fixtures.deleteAll();
        Fixtures.load("yamlTestData.yml");
        YamlModel ym = YamlModel.all().first();
        assertEquals("Morten", ym.name);

        assertEquals(DataWithCompositeKey.all().fetch().size(), 2);

        //check binary
        assertEquals("This String is stored in yaml file using base64", new String(ym.binaryData));
        
    }
    
}
