import org.junit.Test;

import play.test.UnitTest;
import play.utils.Utils;


public class UtilsTest extends UnitTest {
    @Test
    public void dropRevision() {
        assertEquals("module", Utils.dropRevision("module"));
        assertEquals("module", Utils.dropRevision("module-head"));
        assertEquals("module", Utils.dropRevision("module-1.0"));
        assertEquals("module", Utils.dropRevision("module-1.0beta"));
        assertEquals("long-module", Utils.dropRevision("long-module"));
        assertEquals("long-module", Utils.dropRevision("long-module-head"));
        assertEquals("long-module", Utils.dropRevision("long-module-1.0"));
        assertEquals("long-module", Utils.dropRevision("long-module-1.0beta"));
    }
}
