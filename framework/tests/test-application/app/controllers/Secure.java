package controllers;

import play.mvc.Controller;
import play.mvc.Before;
import play.libs.OpenID;

public class Secure extends Controller {
	
	@Before
	static void checkAuth() throws Exception {
		System.out.println(session);
		if(session.get("openid") == null) {
			OpenID.verify("http://gbo.myopendid.com", "");
		} 
	}
	
	public static void authenticate() { 
		
	}
	
	public static void index() {
		renderText("Hello");
	}
	
}