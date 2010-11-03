package models;

import javax.persistence.*;
import java.util.*;

import play.db.jpa.*;
import play.data.validation.*;

@Entity
public class Forum extends Model {

    @Required
    public String name;
    public String description;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "forum")
    public List<Topic> topics;
    
    // ~~~~~~~~~~~~ 
    
    public Forum(String name, String description) {
        this.name = name;
        this.description = description;
        create();
    }
    
    // ~~~~~~~~~~~~ 
    
    public Topic newTopic(User by, String subject, String content) {
        Topic t = new Topic(this, by, subject, content);
        this.refresh();
        return t;
    }
    
    // ~~~~~~~~~~~~ 
    
    public long getTopicsCount() {
        return Topic.count("forum", this);
    }

    public long getPostsCount() {
        return Post.count("topic.forum", this);
    }

    public List<Topic> getTopics(int page, int pageSize) {
        return Topic.find("forum", this).fetch(page, pageSize);
    }

    public Post getLastPost() {
        return Post.find("topic.forum = ? order by postedAt desc", this).first();
    }
    
}

