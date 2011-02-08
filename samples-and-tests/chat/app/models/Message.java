package models;

public class Message {

    public String user;
    public String text;
    
    private Message(String user, String text) {
        this.user = user;
        this.text = text;
    }
    
    static Message on(String user, String text) {
        return new Message(user, text);
    }
    
    static Message on(String user, String text, String... args) {
        return new Message(user, String.format(text, args));
    }
    
}

