import org.junit.*;
import play.test.*;
import play.cache.*;
import java.util.*;
import play.libs.WS;

import models.*;

public class CustomEncodingTest extends UnitTest {

    @Test
    public void testCustomEncoding() {
        Assert.assertEquals("Norwegian letters: ÆØÅ", 
            WS.url("http://localhost:9003/customEncoding/getText").get().getString("iso-8859-1"));
        Assert.assertEquals("Norwegian letters: ÆØÅ", 
            WS.url("http://localhost:9003/customEncoding/getTemplate").get().getString("iso-8859-1"));
    }
}