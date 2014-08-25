package controllers.t3;
import cn.bran.play.JapidController;
import cn.bran.play.JapidPlayAdapter;
import cn.bran.play.routing.AutoPath;
import cn.bran.play.routing.HttpMethod.GET;
import cn.bran.play.routing.HttpMethod.POST;
import cn.bran.play.routing.EndWith;

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
		renderText("hi: " + a +":" + b + ". reverse: " + JapidPlayAdapter.lookup("t3.App.bb", new Object[] {a, b}));
	}

	// effectively -> *  /t3.App.tee/{a}/{b}
	public static void tee(int a, String b) {
		renderJapid(a, b);
	}
}
