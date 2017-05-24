package models;

import javax.persistence.*;
import java.util.*;

import play.*;
import play.db.jpa.*;
import play.libs.*;
import play.data.validation.*;

@Entity
public class User extends Model {

    @Email
    @Required
    public String email;
    
    @Required
    public String passwordHash;
    
    @Required
    public String name;
    
    public String needConfirmation;
    
    // ~~~~~~~~~~~~ 
    
    public User(String email, String password, String name) {
        this.email = email;
        this.passwordHash = Codec.hexMD5(password);
        this.name = name;
        this.needConfirmation = Codec.UUID();
        create();
    }
    
    // ~~~~~~~~~~~~ 
    
    public boolean checkPassword(String password) {
        return passwordHash.equals(Codec.hexMD5(password));
    }

    public boolean isAdmin() {
        return email.equals(Play.configuration.getProperty("forum.adminEmail", ""));
    }
    
    // ~~~~~~~~~~~~ 
    
    public List<Post> getRecentsPosts() {
        return Post.find("postedBy = ?1 order by postedAt", this).fetch(1, 10);
    }

    public Long getPostsCount() {
        return Post.count("postedBy", this);
    }

    public Long getTopicsCount() {
        return Post.count("select count(distinct t) from Topic t, Post p, User u where p.postedBy = ?1 and p.topic = t", this);
    }
    
    // ~~~~~~~~~~~~ 
    
    public static User findByEmail(String email) {
        return find("email", email).first();
    }

    public static User findByRegistrationUUID(String uuid) {
        return find("needConfirmation", uuid).first();
    }

    public static List<User> findAll(int page, int pageSize) {
        return User.all().fetch(page, pageSize);
    }

    public static boolean isEmailAvailable(String email) {
        return findByEmail(email) == null;
    }
    
}

