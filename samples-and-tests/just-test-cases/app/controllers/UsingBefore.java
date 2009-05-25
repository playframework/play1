package controllers;

import models.*;
import play.*;
import play.mvc.*;
import java.util.*;

public class UsingBefore extends Controller {

    @Before(unless="a")
    static void yop(String name) {
        renderText("Yop " + name);
    }
    
    public static void a(String name) {
        renderText("Youhou " + name);
    }
    
    public static void b(String name) {
        renderText("Youhou " + name);
    }
    
}

