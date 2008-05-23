package controllers;

import play.Logger;
import play.mvc.Controller;

public class Application extends Controller {
	
	public static void index() {
		Logger.info("From controller -> %s", name_());
	}
	
	static String name_() {
		return "Guillaume";
	}
	
}
