import java.io.File;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

import play.Logger;
import play.libs.WS;
import play.libs.WS.FileParam;
import play.libs.WS.HttpResponse;
import play.mvc.Http.Header;
import play.test.UnitTest;

import com.google.gson.JsonObject;
import controllers.Rest;


public class DataBindingUnitTest extends UnitTest {
    
    
    @Test
    public void testByteBinding() throws Exception{
        File fileToSend = new File(new URLDecoder().decode(getClass().getResource("/kiki.txt").getFile(), "UTF-8"));
        assertEquals("b.ba.length=749", WS.url("http://localhost:9003/DataBinding/bindBeanWithByteArray").files(new FileParam(fileToSend, "b.ba")).post().getString());
        
    }

}
