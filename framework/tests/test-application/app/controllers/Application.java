package controllers;

import play.mvc.Controller;
import java.util.HashSet;
import java.util.Date;

public class Application extends Controller {

    public static void index(HashSet name, Integer age, HashSet<Date> date) {
        renderText("SHIGETA フローラルウォーターは (%s -> %s / %s)", name, age, date);
    }
    
    public static void test() {
        redirect("/");
    }

}
