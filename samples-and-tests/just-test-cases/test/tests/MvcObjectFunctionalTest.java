package tests;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.*;
import org.junit.Before;

import controllers.async.AsyncApplication;

import play.Logger;
import play.test.*;
import play.mvc.*;
import play.mvc.Scope.RenderArgs;
import play.mvc.Http.*;
import models.*;
import models.async.AsyncTrace;

public class MvcObjectFunctionalTest extends FunctionalTest {
    @Test
    public void testParam(){
        Request request = Request.current();
        assertNotNull(request);
        
        Response response = Response.current();
        assertNotNull(response);
        
        RenderArgs renderArgs = RenderArgs.current();
        assertNotNull(renderArgs);
         
        Request r = newRequest();
        
        r.params.put("a", "b"); 
        assertEquals("b", r.params.get("a"));
     }  
}