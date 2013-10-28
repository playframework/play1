import org.junit.*;
import play.test.*;
import models.*;

public class YamlTest extends UnitTest {
    
    @Test
    public void testYamlLoading() {
        Fixtures.deleteAll();
        Fixtures.load("yamlTestData.yml");
        YamlModel ym = YamlModel.findById(1L);
        assertEquals("Morten", ym.name);

        assertEquals(DataWithCompositeKey.all().fetch().size(), 2);

        //check binary
        assertEquals("This String is stored in yaml file using base64", new String(ym.binaryData));
        
    }
    
    @Test
    public void testYamlLoadingNewMethods() {
        Fixtures.deleteDatabase();
        Fixtures.loadModels("yamlTestData.yml");
        YamlModel ym = YamlModel.findById(1L);
        assertEquals("Morten", ym.name);

        assertEquals(DataWithCompositeKey.all().fetch().size(), 2);

        //check binary
        assertEquals("This String is stored in yaml file using base64", new String(ym.binaryData));     
    }
    
    @Test
    public void testYamlLoadingAsTemplate() {
        Fixtures.deleteDatabase();
        Fixtures.loadModels(true, "yamlTestData.yml");
        YamlModel ym = YamlModel.findById(2L);
        assertEquals("message.name.yamlModel2[]-Test", ym.name);   
    }
    
    @Test
    public void testYamlLoadingNotAsTemplate() {
        Fixtures.deleteDatabase();
        Fixtures.loadModels(false, "yamlTestData.yml");
        YamlModel ym = YamlModel.findById(2L);
        assertEquals("&{'message.name.yamlModel2'}[${toto}]-Test", ym.name);
    } 
    
    @Test
    public void testMultipleYamlLoadingAsTemplate() {
        Fixtures.deleteDatabase();
        Fixtures.loadModels(true, "yamlTestData.yml", "yamlTestData2.yml");
        YamlModel ym = YamlModel.findById(2L);
        assertEquals("message.name.yamlModel2[]-Test", ym.name);   
        ym = YamlModel.findById(4L);
        assertEquals("message.name.yamlModel4[]-Test", ym.name);   
    }
    
    @Test
    public void testMultipleYamlLoadingNotAsTemplate() {
        Fixtures.deleteDatabase();
        Fixtures.loadModels(false, "yamlTestData.yml", "yamlTestData2.yml");
        YamlModel ym = YamlModel.findById(2L);
        assertEquals("&{'message.name.yamlModel2'}[${toto}]-Test", ym.name);
        ym = YamlModel.findById(4L);
        assertEquals("&{'message.name.yamlModel4'}[${toto}]-Test", ym.name);   
    } 
}
