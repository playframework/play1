package models;

import java.util.*;

public class Message {

    public String user;
    public String text;
    
    public static Message on(String user, String text) {
        Message m = new Message();
        m.user = user;
        m.text = text;
        return m;
    }
    
}

