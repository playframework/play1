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
import play.exceptions.UnexpectedException;
import models.*;
import models.async.AsyncTrace;

@CleanTest(removeCurrent= true, createDefault=false)
public class FunctionalTestCleanTest extends FunctionalTest {
    @Test
    public void testParam(){
        Request request = Request.current();
        assertNull(request);
     
        Response response = Response.current();
        assertNull(response);
      
        RenderArgs renderArgs = RenderArgs.current();
        assertNull(renderArgs);
     }  
}