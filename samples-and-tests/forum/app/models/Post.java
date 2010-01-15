package models;

import play.data.binding.annotations.As;

import javax.persistence.*;
import java.util.*;

import play.db.jpa.*;

@Entity
public class Post extends Model {

    public String content;
    @As("yyyy-MM-dd")
    public Date postedAt;
    
    @OneToOne
    public User postedBy;
    
    @ManyToOne
    public Topic topic;
    
    // ~~~~~~~~~~~~ 
    
    public Post(Topic topic, User postedBy, String content) {
        this.topic = topic;
        this.postedBy = postedBy;
        this.content = content;
        this.postedAt = new Date();
        save();
    }
    
}

