package controllers;

import cn.bran.play.JapidController;
import cn.bran.play.routing.AutoPath;
import cn.bran.play.routing.HttpMethod.GET;


@AutoPath
public class AutoPather extends JapidController{
	
	@GET
	public static void foo(String a, int b) {
		renderJapid(a, b);
	}
	
	public static void bar(String a, int b) {
		renderText("bar");
	}
	
}
