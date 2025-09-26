import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import play.test.*;
import play.cache.*;
import java.util.*;
import play.libs.WS;

import models.*;

public class CustomEncodingTest extends UnitTest {

    @Test
    public void testCustomEncoding() {
        Assertions.assertEquals("Norwegian letters: ÆØÅ",
            WS.url("http://localhost:9003/customEncoding/getText").get().getString("iso-8859-1"));
        Assertions.assertEquals("Norwegian letters: ÆØÅ",
            WS.url("http://localhost:9003/customEncoding/getTemplate").get().getString("iso-8859-1"));
    }
}