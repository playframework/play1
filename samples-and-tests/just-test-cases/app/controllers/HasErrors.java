package controllers;

import models.*;
import play.*;
import play.mvc.*;
import java.util.*;

public class HasErrors extends Controller {

    public static void willBeNotFound() {
        notFound("Hop");
    }
    
    public static void willThrowError() {
        int a = 9/0;
        renderText(a);
    }

    /**
     * This test fix for #478315
     */
    public static void willThrowErrorToo() {
        render("/HasErrors/index.html");
    }
}

