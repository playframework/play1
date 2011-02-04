package models;

import java.util.*;
import java.util.concurrent.atomic.*;

public class Message {
    
    static AtomicLong uid = new AtomicLong(1);

    public String user;
    public String text;
    public Long id;
    
    public static Message on(String user, String text) {
        Message m = new Message();
        m.user = user;
        m.text = text;
        m.id = uid.getAndIncrement();
        return m;
    }
    
    public String toString() {
        return "[" + id + "] " + user + ": " + text;
    }
    
}

