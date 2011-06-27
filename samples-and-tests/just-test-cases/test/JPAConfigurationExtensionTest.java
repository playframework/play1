import config.MatchAllJPAConfigurationExtension;
import config.MatchNoJPAConfigurationExtension;
import config.MatchOneJPAConfigurationExtension;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Tests if the {@link play.db.jpa.JPAConfigurationExtension}-s are invoked correctly.
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class JPAConfigurationExtensionTest extends UnitTest {

    @Test
    public void testInvokeOnAll() {
        assertEquals(2, MatchAllJPAConfigurationExtension.configurations.size());
    }

    @Test
    public void testInvokeOne() {
        assertEquals(1, MatchOneJPAConfigurationExtension.configuration.size());
    }

    @Test
    public void testInvokeNone() {
        assertEquals(0, MatchNoJPAConfigurationExtension.configuration.size());
    }


}
