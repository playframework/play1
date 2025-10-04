package models;

import jakarta.persistence.*;
import java.util.*;

import play.data.binding.*;

import play.db.jpa.*;

@Entity
public class Post extends Model {

    public String content;
    
    @As("yyyy-MM-dd")
    public Date postedAt;
    
    @ManyToOne
    public User postedBy;
    
    @ManyToOne
    public Topic topic;
    
    // ~~~~~~~~~~~~ 
    
    public Post(Topic topic, User postedBy, String content) {
        this.topic = topic;
        this.postedBy = postedBy;
        this.content = content;
        this.postedAt = new Date();
        create();
    }
    
}

