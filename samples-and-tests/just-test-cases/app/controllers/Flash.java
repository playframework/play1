package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class Flash extends Controller {

    public static void index() {
        render();
    }
    
    public static void addNow() {
        flash.now("msg", "Yop");
        render("@index");
    }
    
    public static void add() {
        flash.put("msg", "Yop");
        render("@index");
    }
    
    public static void keep() {
        flash.keep("msg");
        render("@index");
    }
    
}

