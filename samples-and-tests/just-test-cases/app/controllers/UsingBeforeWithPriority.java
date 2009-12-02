package controllers;

import models.*;
import play.*;
import play.mvc.*;
import java.util.*;

public class UsingBeforeWithPriority extends Controller {

    @Before(priority=4)
    static void yop() {
        response.print("yop");
    }

    @Before(priority=12)
    static void yip() {
        response.print("yip");
    }    
    
    public static void index() {
        response.print("done");
    }
    
    @Before(priority=2)
    static void yup() {
        response.print("yup");
    }
    
    @Before(priority=1000)
    static void yap() {
        response.print("yap");
    }
    
}

