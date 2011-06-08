
import play.*;
import org.junit.Test;
import play.test.UnitTest;
import java.io.File;

public class ConfigTest extends UnitTest {

    @Test
    public void testIncludedConfig() {
        assertEquals("a", Play.configuration.get("included_a"));
        assertNull(Play.configuration.get("%test.included_b"));
        assertEquals("b", Play.configuration.get("included_b"));
        assertEquals(Play.frameworkPath, new File((String)Play.configuration.get("included_c")));
    }
}