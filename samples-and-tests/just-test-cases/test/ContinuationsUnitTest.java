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

public class ContinuationsUnitTest extends UnitTest {


    @Test
    public void testParamsAfterAwait() throws Exception {
        
        // test POST
        Map<String, Object> p = new HashMap<String, Object>();
        p.put("a", "morten");
        p.put("b","34");
        assertEquals("before await: a: morten b: 34 params[a]: morten params[b]: 34\nafter await: a: morten b: 34 params[a]: morten params[b]: 34", 
            WS.url("http://localhost:9003/withContinuations/echoParamsAfterAwait").params(p).post().getString());
            
        // Test GET
        assertEquals("before await: a: morten b: 34 params[a]: morten params[b]: 34\nafter await: a: morten b: 34 params[a]: morten params[b]: 34", 
            WS.url("http://localhost:9003/withContinuations/echoParamsAfterAwait?a=morten&b=34").get().getString());

    }
    
    @Test
    public void testValidationAndAwait() throws Exception {
        assertEquals("beforeErrors: a=Required,b=someError afterErrors: a=Required,b=someError,sb.prop=Required,sb=Validation failed", 
            WS.url("http://localhost:9003/withContinuations/validationAndAwait").get().getString());
    }
    
    @Test
    public void testParamsLocalVariableTracerAndAwait() {
        assertEquals("a:12-aa:12", 
            WS.url("http://localhost:9003/withContinuations/paramsLocalVariableTracerAndAwait?a=12").get().getString());
    }
    
    @Test
    public void testInvalidBindingAndAwait() {
        assertEquals("beforeErrors: b=Incorrect value,b=someError afterErrors: b=Incorrect value,b=someError,sb.prop=Required,sb=Validation failed",
            WS.url("http://localhost:9003/WithContinuations/validationAndAwait?a=morten&b=x").get().getString());
    }
    
}