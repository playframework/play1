package controllers.more;

import models.DataModel;

// make sure you have 
// 		module.japid=${play.path}/modules/japid-head
// in your application.conf file, and "play eclipsify"
// if you notice the JapidController is not found.

public class MyController extends BaseController {

	public static void index() {
		renderJapid("Hello world!", 123);
	}

	public static void echo(String m) {
		validation.required("m", m);
		renderJapid("m", 123);
	}

	public static void subview() {
		renderJapid("subviews....");
	}

	public static void quickview() {
		renderJapid();
	}

	public static void scriptline() {
		renderJapid();
	}

	public static void doBodyInDef() {
		renderJapid();
	}

	
	public static void dobodytest() {
		renderJapid();
	}
	
	
}
