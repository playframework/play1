package controllers;

import models.*;
import play.*;
import play.mvc.*;
import java.util.*;

public class UsingBefore extends Controller {

    @Before(unless = {"a", "onlytest"})
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
}
