import org.junit.*;
import java.util.*;
import play.test.*;
import models.*;

import play.libs.WS;

import play.*;

public class BasicTest extends UnitTest {

    @Test
    public void aVeryImportantThingToTest() {
            Logger.info("Trying to retrieve settings");
        assertEquals("myValue", WS.url("http://localhost:9000/").get().getString());
    }

}
