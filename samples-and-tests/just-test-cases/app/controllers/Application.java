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
    
    public static void mail() {
        notifiers.Welcome.welcome();
        renderText("OK");
    }
    
    public static void mail2() {
        Welcome.welcome();
        renderText("OK2");
    }
    
    public static void ifthenelse() {
        boolean a = true;
        boolean b = false;
        String c = "";
        String d = "Yop";
        int e = 0;
        int f = 5;
        Boolean g = null;
        Boolean h = true;
        Object i = null;
        Object j = new Object();
        render(a,b,c,d,e,f,g,h,i,j);
    }
    
    public static void a() {
        render();
    }

}