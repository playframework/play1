package controllers.more;

import cn.bran.play.JapidController;
import cn.bran.play.JapidController2;
import play.mvc.Before;

public class BaseController extends JapidController2 {
    @Before
    static void beforeHandler() {
    	System.out.println("in before handler");
    }
	
}
