package controllers;

import play.*;
import play.mvc.*;
import play.data.validation.*;

import java.util.*;

import models.*;

public class Application extends Controller {

    public static void index() {
        render();
    }
    
    public static void join(@Required String user) {
        if(validation.hasErrors()) {
            flash.error("Please choose a nick name…");
            index();
        }
        session.put("user", user);
        ChatRoom.get().talk(Message.on("notice", user + " has joined the room"));
        room();
    }
    
    public static void room() {
        render();
    }
    
    public static void say(String msg) {
        if(!session.contains("user")) {
            forbidden();
        }
        if(msg != null && msg.trim().length() > 0) {
            ChatRoom.get().talk(Message.on(session.get("user"), msg));
        }        
    }
    
    public static void waitMessages(Long lastReceived) {
        List<Message> messages = wait(ChatRoom.get().nextMessages(lastReceived));
        renderJSON(messages);
    }

}