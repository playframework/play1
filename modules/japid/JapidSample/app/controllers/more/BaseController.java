package controllers.more;

import cn.bran.play.JapidController;
import play.mvc.Before;

public class BaseController extends JapidController {
    @Before
    static void beforeHandler() {
    	System.out.println("in the before handler");
    }
	
}
