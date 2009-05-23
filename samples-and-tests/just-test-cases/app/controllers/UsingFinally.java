package controllers;

import models.*;
import play.*;
import play.mvc.*;
import java.util.*;

public class UsingFinally extends Controller {

    @Finally(unless="a")
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
    
}

