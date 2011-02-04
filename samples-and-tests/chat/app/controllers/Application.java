package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class Application extends Controller {

    public static void index() {
        render();
    }
    
    public static void join(String user) {
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
        ChatRoom.get().talk(Message.on(session.get("user"), msg));
    }
    
    public static void waitMessage() {
        Message msg = wait(ChatRoom.get().nextMessage());
        renderJSON(msg);
    }

}