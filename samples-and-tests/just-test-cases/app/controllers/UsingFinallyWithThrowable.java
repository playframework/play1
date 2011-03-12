package controllers;

import play.*;
import play.mvc.*;
import play.db.jpa.*;
import play.libs.*;
import play.libs.F.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import models.*;

public class UsingFinallyWithThrowable extends Controller {
    
    public static boolean finally1Called = false;
    public static boolean finally2Called = false;
    public static boolean finally2Called_withException = false;
    public static boolean finally3Called = false;
    
    private static void resetFlags(){
        //reset finally-flags
        finally1Called = false;
        finally2Called = false;
        finally2Called_withException = false;
        finally3Called = false;
    }
    
    public static void noError(){
        resetFlags();
    }
    
    public static void withError() throws Exception{
        resetFlags();
        throw new Exception("Something bad happened");
    }
    
    @Finally
    public static void myFinally(){
        finally1Called = true;
    }
    
    @Finally
    public static void myFinally(Throwable e){
        finally2Called = true;
        if( e != null ){
            Logger.trace("got exception via finally", e);
            finally2Called_withException = true;
        }
    }
    
    //some finally method with "random" args
    @Finally
    public static void myFinally(String a, String b){
        finally3Called = true;
    }
    
    public static void get_results(){
        String result = "" + finally1Called + " " + finally2Called + " " + finally2Called_withException + " " + finally3Called;
        //Logger.info("Result: " + result);
        renderText(result);
    }
    
}

