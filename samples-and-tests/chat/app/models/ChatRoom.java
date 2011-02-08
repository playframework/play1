package models;

import java.util.*;

import play.libs.*;
import play.libs.F.*;

public class ChatRoom {
    
    // Let's chat! 
    
    final ArchivedEventStream<Message> messages = new ArchivedEventStream<Message>(100);
    
    public EventStream<Message> join(String user) {
        messages.publish(Message.on("notice", "%s has joined the room", user));
        return messages.eventStream();
    }
    
    public void leave(String user) {
        messages.publish(Message.on("notice", "%s has left the room", user));
    }
    
    public void say(String user, String message) {
        if(message == null || message.trim().equals("")) {
            return;
        }
        messages.publish(Message.on(user, message));
    }
    
    public Promise<List<Event<Message>>> nextMessages(long lastReceived) {
        return messages.nextEvents(lastReceived);
    }
    
    public List<Message> archive() {
        return messages.archive();
    }
    
    // Factory

    static ChatRoom instance = null;
    public static ChatRoom get() {
        if(instance == null) {
            instance = new ChatRoom();
        }
        return instance;
    }
    
}

