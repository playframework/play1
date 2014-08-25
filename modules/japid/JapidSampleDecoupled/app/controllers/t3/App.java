package controllers.t3;
import cn.bran.play.JapidController;
import cn.bran.play.routing.AutoPath;
import cn.bran.play.routing.EndWith;
import cn.bran.play.routing.HttpMethod.GET;
import cn.bran.play.routing.HttpMethod.POST;

@AutoPath // effectively == @AutoPath("/t3.App")
public class App extends JapidController {
	
	// effective path -> * /t3.App.foo
	public static void foo() {
		renderText("hi foo  ");
	}

	// effectively -> * /t3.App.ff. 
	// The param s will be taken from query string 
	@AutoPath(".ff") 
	public static void fff(String s) {
		renderText("hi fff: " + s);
	}
	
	// effectively -> GET|POST  /t3.App.bb/{a}/{b}.html
	@GET
	@POST
	@EndWith // ".html" by default
	public static void bb(int a, String b) {
		renderJapid(a, b);
	}

	// effectively -> *  /t3.App.tee
	// POST method do not take args from path by convention
	@POST
	public static void tee(int a, String b) {
		renderText("tee: " + a +"::" + b);
	}
}
