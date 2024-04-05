package tests;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.*;
import org.junit.Before;


import play.Logger;
import play.test.*;
import play.mvc.*;
import play.mvc.Scope.RenderArgs;
import play.mvc.Http.*;
import play.exceptions.UnexpectedException;
import models.*;

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