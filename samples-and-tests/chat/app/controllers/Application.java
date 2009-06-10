package controllers;

import java.util.*;

import play.*;
import play.mvc.*;
import play.libs.*;
import play.data.validation.*;

public class Application extends Controller {
    
    @Before(unless={"signin", "register"})
    static void checkLogged() {
        if(!session.contains("nick")) {
            signin();
        }
    }

    // ~~

	public static void index() {
		render();
	}
	
	public static void post(String message) {
	    new Message(session.get("nick"), message).save();
	}
	
	// ~~ login
	
	public static void signin() {
	    render();
	}
	
	public static void register(@Required String nick) {
	    if(validation.hasErrors()) {
	        flash.error("Please give a nick name");
	        signin();
	    }
	    session.put("nick", nick);
	    index();
	}
	
	public static void disconnect() {
	    session.clear();
	    signin();
	}

}