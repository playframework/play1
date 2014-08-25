package controllers.more;

import cn.bran.play.JapidController;
import cn.bran.play.JapidController2;

public class SubController extends JapidController2 {
	public static void foo(String what) {
		renderJapid(what);
	}
}
