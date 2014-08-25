package controllers.more;

import models.DataModel;

// make sure you have 
// 		module.japid=${play.path}/modules/japid-head
// in your application.conf file, and "play eclipsify"
// if you notice the JapidController is not found.

public class ControllerJdk7 extends BaseController {

	public static void index() {
//		String a = "a";
//		switch (a) {
//		case "a":
//			System.out.println("a");
//			break;
//		default:
//			System.out.println("b");
//			break;
//		}
		renderJapid("b");
	}
	
	public static void a() {
		renderJapid();
	}
	public static void b() {
		renderJapid();
	}
	public static void c() {
		renderJapid();
	}
	public static void d() {
		renderJapid();
	}
	public static void e() {
		renderJapid();
	}
}
