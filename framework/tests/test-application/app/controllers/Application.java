package controllers;

import play.mvc.Controller;
import play.Logger;
import play.mvc.Before;
import java.util.HashSet;
import java.util.Date;

public class Application extends Controller {
	
	@Before
	static void log(String name) {
		Logger.info(">>>> %s", name);
		if(name.equals("Guillaume")) {
			redirect("http://www.google.com");
		}
	}

    public static void index(String name, Integer age, HashSet<Date> date) {
        renderText("SHIGETA フローラルウォーターは (%s -> %s / %s)", name, age, date);
    }
    
    public static void test() {
        Orders.show(56L);
    }

}
