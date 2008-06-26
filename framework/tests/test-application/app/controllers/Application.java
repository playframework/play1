package controllers;

import play.mvc.Controller;
import play.Logger;
import play.Invoker;
import play.mvc.Before;
import java.util.HashSet;
import java.util.Date;

public class Application extends Controller {

    @Before
    static void log(String name, Integer age) {
        Logger.info(">>>> %s", name);
        if ("Guillaume".equals(name)) {
            redirect("http://www.amazon.com");
        }
        models.Test.test();
        if(name != null) {
            session.put("name", name);
        }  
    }

    public static void index(String name, Integer age, HashSet<Date> date) { 
        models.Test t = new models.Test();
        t.prop = "kiki";
        Yop.kiki();
        renderText("%s | SHIGETA フローラルウォーターは (%s -> %s / %s)", t.prop, name, age, date);        
    }

    public static void test() {
        Orders.test("Guillaume");
    }
    
}
