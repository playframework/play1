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
    
    public static void room(@Required String user) {
        if(validation.hasErrors()) {
              flash.error("Please choose a nick name…");
              index();
        }
        ChatRoom.get().talk(Message.on("notice", user + " has joined the room"));
        render(user);
    }
    
    public static void say(String user, String msg) {
        if(msg != null && msg.trim().length() > 0) {
            ChatRoom.get().talk(Message.on(user, msg));
        }        
    }
    
    public static void waitMessages(Long lastReceived) {
        List<Message> messages = await(ChatRoom.get().nextMessages(lastReceived));
        renderJSON(messages);
    }

}