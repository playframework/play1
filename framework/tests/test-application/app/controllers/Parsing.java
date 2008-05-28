package controllers;

import java.io.File;
import play.mvc.Controller;

public class Parsing extends Controller {

    public static void index () {
        render();
    }
    
    public static void post (String toto, File truc) {
        render(toto,truc);
    }
    
}
