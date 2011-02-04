package models;

import java.util.*;
import java.util.concurrent.*;

import play.libs.*;

public class ChatRoom {
    
    // Factory

    static ChatRoom instance = null;
    static {
        instance = new ChatRoom();
    }
    
    public static ChatRoom get() {
        return instance;
    }
    
    // Let's chat!
    
    Queue<Task<Message>> waiting = new ConcurrentLinkedQueue<Task<Message>>();
    
    public void talk(Message msg) {
        while(!waiting.isEmpty()) {
            waiting.poll().invoke(msg);
        }
    }
    
    public Task<Message> nextMessage() {
        Task<Message> futureMessage = new Task<Message>();
        waiting.offer(futureMessage);
        return futureMessage;        
    } 
    
}

