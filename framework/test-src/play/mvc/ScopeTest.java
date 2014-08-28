package play.mvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.*;

import play.PlayBuilder;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Params;


public class ScopeTest {
    
    @org.junit.Before
    public void playBuilderBefore() {
        new PlayBuilder().build();
    }

    private static void mockRequestAndResponse() {
        Request.current.set(new Request());
        Response.current.set(new Response());
    }

    @Test
    public void testParamsPut() { 
        mockRequestAndResponse();
        Params params = new Params();
        params.put("param1", "test");
        params.put("param1.test", "test2");
        
        params.put("param1.object", "obj");
        params.put("param1.object.param1", "param1");
        params.put("param1.object.param2", "param2");
        params.put("param1.object.param2.3", "param3");
        
        assertEquals(6, params.all().size());
        
        assertTrue(params._contains("param1"));
        assertTrue(params._contains("param1.object"));     
        assertTrue(params._contains("param1.test"));       
        assertTrue(params._contains("param1.object.param1"));
        assertTrue(params._contains("param1.object.param2"));
        assertTrue(params._contains("param1.object.param2.3"));
    }
    
    @Test
    public void testParamsRemove() {
        mockRequestAndResponse();
        Params params = new Params();
        params.put("param1", "test");
        params.put("param1.test", "test2");
        
        params.put("param1.object", "obj");
        params.put("param1.object.param1", "param1");
        params.put("param1.object.param2", "param2");
        params.put("param1.object.param2.3", "param3");
        
        assertEquals(6, params.all().size());
        
        params.remove("param1.object.param2");
    
        assertTrue(params._contains("param1"));
        assertTrue(params._contains("param1.test"));
        assertTrue(params._contains("param1.object")); 
        assertTrue(params._contains("param1.object.param1"));
        assertFalse(params._contains("param1.object.param2"));
        assertTrue(params._contains("param1.object.param2.3"));
        
        assertEquals(5, params.all().size());
    }
    
    @Test
    public void testParamsRemove2() {
        mockRequestAndResponse();
        Params params = new Params();
        params.put("param1", "test");
        params.put("param1.test", "test2");
        
        params.put("param1.object", "obj");
        params.put("param1.object.param1", "param1");
        params.put("param1.object.param2", "param2");
        params.put("param1.object.param2.3", "param3");
        
        assertEquals(6, params.all().size());
        
        params.remove("param1.object");
    
        assertTrue(params._contains("param1"));
        assertTrue(params._contains("param1.test"));
        assertFalse(params._contains("param1.object")); 
        assertTrue(params._contains("param1.object.param1"));
        assertTrue(params._contains("param1.object.param2"));
        assertTrue(params._contains("param1.object.param2.3"));
        
        assertEquals(5, params.all().size());
    }
    
    @Test
    public void testParamsRemoveStartWith() {
        mockRequestAndResponse();
        Params params = new Params();
        params.put("param1", "test");
        params.put("param1.test", "test2");
        
        params.put("param1.object", "obj");
        params.put("param1.object.param1", "param1");
        params.put("param1.object.param2", "param2");
        params.put("param1.object.param2.3", "param3");
        
        assertEquals(6, params.all().size());
        
        params.removeStartWith("param1.object");
    
        assertTrue(params._contains("param1"));
        assertTrue(params._contains("param1.test"));
        assertFalse(params._contains("param1.object"));       
        assertFalse(params._contains("param1.object.param1"));
        assertFalse(params._contains("param1.object.param2"));
        assertFalse(params._contains("param1.object.param2.3"));
        
        assertEquals(2, params.all().size());
    }
}
