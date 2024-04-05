import java.io.File;
import java.net.URLDecoder;

import org.junit.Test;

import play.libs.WS;
import play.libs.WS.FileParam;
import play.test.UnitTest;


public class DataBindingUnitTest extends UnitTest {
    
    
    @Test
    public void testByteBinding() throws Exception{
        File fileToSend = new File(URLDecoder.decode(getClass().getResource("/kiki.txt").getFile(), "UTF-8"));
        assertEquals("b.ba.length=749", WS.url("http://localhost:9003/DataBinding/bindBeanWithByteArray").files(new FileParam(fileToSend, "b.ba")).post().getString());  
    }

}
