package controllers;

import play.*;
import play.mvc.*;
import play.cache.*;

import java.util.*;

import models.*;

public class UseCache extends Controller {

    public static void index() {
        render();
    }
    
    @CacheFor("2s")
    public static void getDate() {
        Date a = new Date();
        render(a);
    }
    
}

