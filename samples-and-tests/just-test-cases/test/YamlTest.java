import org.junit.*;
import play.test.*;
import models.*;

public class YamlTest extends UnitTest {

    @Test
    public void testYamlLoading() {
        Fixtures.deleteAll();
        Fixtures.load("yamlTestData.yml");
        YamlModel ym = YamlModel.all().first();
        assertEquals("Morten", ym.name);

        //check binary
        assertEquals("This String is stored in yaml file using base64", new String(ym.binaryData));

    }

}
