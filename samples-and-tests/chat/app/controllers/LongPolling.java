package controllers;

import play.*;
import play.mvc.*;
import play.libs.F.*;

import java.util.*;
import com.google.gson.reflect.*;

import models.*;

public class LongPolling extends Controller {

    public static void room(String user) {
        ChatRoom.get().join(user);
        render(user);
    }
    
    public static void say(String user, String message) {
        ChatRoom.get().say(user, message);
    }
    
    public static void waitMessages(Long lastReceived) {        
        // Here we use continuation to suspend 
        // the execution until a new message has been received
        List messages = await(ChatRoom.get().nextMessages(lastReceived));
        renderJSON(messages, new TypeToken<List<IndexedEvent<ChatRoom.Event>>>() {}.getType());
    }
    
    public static void leave(String user) {
        ChatRoom.get().leave(user);
        Application.index();
    }
    
}

