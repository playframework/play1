package models;

import javax.persistence.*;
import java.util.*;

import play.db.jpa.*;
import play.data.validation.*;

@Entity
public class Topic extends Model {

    @Required
    public String subject;
    
    public Integer views = 0;
    
    @ManyToOne
    public Forum forum;
    
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "topic")
    public List<Post> posts;
    
    // ~~~~~~~~~~~~ 
    
    public Topic(Forum forum, User by, String subject, String content) {
        this.forum = forum;
        this.subject = subject;
        create();
        new Post(this, by, content);
    }
    
    // ~~~~~~~~~~~~ 
    
    public Post reply(User by, String content) {
        return new Post(this, by, content);
    }
    
    // ~~~~~~~~~~~~ 
    
    public List<Post> getPosts(int page, int pageSize) {
        return Post.find("topic", this).fetch(page, pageSize);
    }

    public Long getPostsCount() {
        return Post.count("topic", this);
    }

    public Long getVoicesCount() {
        return User.count("select count(distinct u) from User u, Topic t, Post p where p.postedBy = u and p.topic = t and t = ?1", this);
    }

    public Post getLastPost() {
        return Post.find("topic = ?1 order by postedAt desc", this).first();
    }
    
}

