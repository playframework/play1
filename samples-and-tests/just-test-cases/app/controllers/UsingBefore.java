package controllers;

import models.*;
import play.*;
import play.mvc.*;
import java.util.*;

public class UsingBefore extends Controller {

    @Before(unless = {"a", "onlytest", "fight"})
    static void yop(String name) {
        renderText("Yop " + name);
    }

    @Before
    public static void kiki() {
        System.out.println("X");
    }

    public static void a(String name) {
        renderText("Youhou " + name);
    }

    public static void b(String name) {
        renderText("Youhou " + name);
    }

    static void isPrivate() {
        renderText("Oops");
    }

    @Before(only = {"onlytest"})
    static void beforeonlytest(String name) {
        renderText("onlywork " + name);
    }

    public static void onlytest(String name) {
        renderText("onlynotwork :  " + name);
    }
    
    public static void fight(String name) {
        name.toString();
        renderText(9/0);
    }
    
    @Catch(ArithmeticException.class)
    static void catchDivByZero(Exception e) {
        renderText("Oops, got " + e);
    }
    
    @Catch(NullPointerException.class)
    static void catchNull(Exception e) {
        renderText("Hey!, got " + e);
    }
}
