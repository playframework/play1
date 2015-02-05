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


public class StaticContentTest extends UnitTest {

    @Test
    public void testGettingStaticFileWithNoneStandardsLetters() {
        // Assure that static content is served as is - that the actuall encoding in the file is seved as it is-
        // The file requested here is stored using iso-8859-1
        // When getting the file, the content-type-encoding in response-header is set according to
        // default response encoding in play, bu this should not affect the bytes transfered..
        assertEquals("NorwegianLetters: æøåÆØÅ", WS.url("http://localhost:9003/public/fileWithNoneStandardLetters_stored_in_iso_8859_1.html").get().getString("iso-8859-1"));
    }
    
    @Test
    public void testGettingStaticFile() {
        assertEquals("test", WS.url("http://localhost:9003/public/session.test?req=1").get().getString("UTF-8"));
    }
    

}
