package controllers.more;

import cn.bran.play.JapidController;

public class SubController extends JapidController {
	public static void foo(String what) {
		renderJapid(what);
	}
}
