package controllers;

import models.*;
import play.*;
import play.mvc.*;
import java.util.*;

public class UsingAfter extends Controller {

    @After(unless = {"a", "onlytest"})
    static void yop(String name) {
        response.reset();
        response.print("YopYop ");
        response.contentType = "text/plain";
    }

    @After(unless= "b")
    static void kiki() {
        response.reset();
        response.print("YopIop ");
        response.contentType = "text/plain";
    }

    public static void a(String name) {
        renderText("Youhou " + name);
    }

    public static void b(String name) {
        renderText("Youhou " + name);
    }

    @After(only = {"onlytest"},priority=Integer.MAX_VALUE)
    static void beforeonlytest(String name) {
        response.reset();
        response.print("onlywork ");
        response.contentType = "text/plain";
    }

    public static void onlytest(String name) {
        renderText("onlynotwork :  " + name);
    }
}
