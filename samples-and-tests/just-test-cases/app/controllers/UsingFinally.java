package controllers;

import models.*;
import play.*;
import play.mvc.*;
import java.util.*;

public class UsingFinally extends Controller {

    @Finally(unless={"a", "onlytest"})
    static void yop(String willBeNull) {
        assert willBeNull == null;
        response.reset();
        response.print("Yop");
        response.contentType = "text/plain";
    }
    
    public static void a() {
        renderText("Youhou");
    }
    
    public static void b() {
        renderText("Youhou");
    }

    @Finally(only = {"onlytest"})
    static void beforeonlytest(String name) {
        response.reset();
        response.print("onlywork");
        response.contentType = "text/plain";
    }

    public static void onlytest(String name) {
        renderText("onlynotwork :  " + name);
    }
  
}

