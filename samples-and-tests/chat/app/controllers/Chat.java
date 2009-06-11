package controllers;

import java.util.*;

import play.*;
import play.mvc.*;
import play.libs.*;
import play.data.validation.*;

import models.*;

public class Chat extends Controller {
    
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
	
	public static void postMessage(String message) {
	    new Message(session.get("nick"), message).save();
	}
	
	public static void newMessages() {
	    List<Message> messages = Message.findBy("date > ?", request.date);
	    if(messages.isEmpty()) {
	        suspend("1s");
	    }
	    renderJSON(messages);
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
	    new Message("notice", nick + " has joined the room").save();
	    index();
	}
	
	public static void disconnect() {
	    new Message("notice", session.get("nick") + " has left the room").save();
        session.clear();
	    signin();
	}

}