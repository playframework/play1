package controllers;

import java.util.*;

import play.*;
import play.mvc.*;
import play.libs.*;

import models.*;

public class Application extends Controller {


    public static void index() {
        render();
    }
    
    public static void hello(String name) {
        render(name);
    }
    
    public static void yop() {
        render();
    }
    
    public static void aGetForm(String name) {
        render("Application/hello.html", name);
    }
    
    public static void aGetForm2(String name) {
        name = "2" + name;
        render("Application/hello.html", name);
    }
    
    public static void optional() {
        renderText("OK");
    }
    
    public static void reverse() {
        render();
    }

}