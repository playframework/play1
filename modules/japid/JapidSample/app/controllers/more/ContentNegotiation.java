package controllers.more;

import play.mvc.*;

import cn.bran.play.JapidController;

public class ContentNegotiation extends JapidController {

    public static void index() {
        renderJapid();
    }

    public static void xml() {
    	renderJapid();
    }
    
    public static void json() {
    	renderJapid();
    }
    
    public static void xmld() {
    	renderXml("<a><b>hello</b></a>");
    }
    
}
