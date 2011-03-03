import org.junit.Before;
import org.junit.Test;

import java.util.*;
import play.Logger;
import play.libs.WS;
import play.libs.WS.FileParam;
import play.libs.WS.HttpResponse;
import play.mvc.Http.Header;
import play.test.UnitTest;

public class ParamsRemoverTest extends UnitTest {

    @Test
    public void testParamsRemove() throws Exception {
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.put( "p1", "v1");
        params.put( "p2", "v2");        
        
        WS.WSRequest r = WS.url("http://localhost:9003/ParamsRemover/getPostPutDeleteOptions_getThenRemove").params(params);
        assertResponse("v1_null", r);
        
        r = WS.url("http://localhost:9003/ParamsRemover/getPostPutDeleteOptions_removeThenGet").params(params);
        assertResponse("v1_null", r);
        
        r = WS.url("http://localhost:9003/ParamsRemover/getPostPutDeleteOptions_remove_in_before_ThenGet").params(params);
        assertResponse("v1_null", r);
    }
    
    protected void assertResponse(String correctReturn, WS.WSRequest r){
        assertEquals(correctReturn, r.get().getString());
        assertEquals(correctReturn, r.post().getString());
        assertEquals(correctReturn, r.put().getString());
        assertEquals(correctReturn, r.delete().getString());
        assertEquals(correctReturn, r.options().getString());
        
    }
    
}