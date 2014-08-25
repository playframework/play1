package controllers.etc;

import play.mvc.*;

import cn.bran.play.*;

// make sure you have 
// 	module.japid=${play.path}/modules/japid-head
// in your application.conf file, and "play eclipsify"
// if you notice the JapidController is not found when importing to Eclipse.

public class MyController extends JapidController {

    public static void list() {
        renderJapid("Hello world!", 123);
    }

}
