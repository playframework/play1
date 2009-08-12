package models;

import java.util.*;
import javax.persistence.*;

import play.db.jpa.*;

@Entity
public class Message extends Model {
    
    public String user;
    public Date date;
    public String text;
    
    public Message(String user, String text) {
        this.user = user;
        this.text = text;
        this.date = new Date();
    }
    
}