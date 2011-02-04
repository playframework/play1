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
    
    // Here we use a messages buffer to be sure to not lost any message
    
    final ArrayBlockingQueue<Message> messagesBuffer = new ArrayBlockingQueue<Message>(100);
    final List<MessagesFilter> waiting = new ArrayList<MessagesFilter>();
    
    public synchronized void talk(Message msg) {
        if(messagesBuffer.remainingCapacity() == 0) {
            messagesBuffer.poll();
        }
        messagesBuffer.offer(msg);
        notifyNewMessages();
    }
    
    public synchronized Task<List<Message>> nextMessages(Long lastReceived) {
        MessagesFilter futureMessages = new MessagesFilter(lastReceived);
        waiting.add(futureMessages);
        notifyNewMessages();
        return futureMessages;        
    } 
    
    public synchronized void notifyNewMessages() {
        for(ListIterator<MessagesFilter> it = waiting.listIterator(); it.hasNext(); ) {
            MessagesFilter filter = it.next();
            for(Message message : messagesBuffer) {
                filter.propose(message);
            }
            if(filter.invoke()) {
                it.remove();
            }
        }
    }
    
    // A custom task that filter only unread messages
    
    static class MessagesFilter extends Task<List<Message>> {
        
        Long lastReceived;
        List<Message> messages = new ArrayList<Message>();
    
        public MessagesFilter(Long lastReceived) {
            this.lastReceived = lastReceived;
        }
        
        public void propose(Message message) {
            if(message.id > lastReceived) {
                messages.add(message);
            }
        }
        
        // If the are messages to dispatch
        // we finish the Task
        public boolean invoke() {
            if(messages.isEmpty()) {
                return false;
            } else {
                super.invoke(messages);
                return true;
            }            
        }
        
    }
    
}

