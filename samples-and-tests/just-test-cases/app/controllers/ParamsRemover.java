package controllers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Before;

public class ParamsRemover extends Controller {

    public static void getPostPutDeleteOptions_getThenRemove(){
        Logger.info("getting, then removing");
        
        String result = "";
        
        String p1 = params.get("p1");
        
        params.remove("p2");
        
        String p2 = params.get("p2");
        
        renderText( p1 + "_" + p2);
        
    }
    
    
    public static void getPostPutDeleteOptions_removeThenGet(){
        Logger.info("getting, then removing");
        
        String result = "";

        params.remove("p2");        
        String p1 = params.get("p1");
        String p2 = params.get("p2");
        
        renderText( p1 + "_" + p2);
        
    }    

    @Before(only="getPostPutDeleteOptions_remove_in_before_ThenGet")
    public static void removeInBefore(){
        params.remove("p2");
    }
    
    public static void getPostPutDeleteOptions_remove_in_before_ThenGet(){
        Logger.info("getting, then removing");
        
        String result = "";

                
        String p1 = params.get("p1");
        String p2 = params.get("p2");
        
        renderText( p1 + "_" + p2);
        
    }    
    
}
