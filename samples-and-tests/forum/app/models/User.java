package models;

import play.*;
import play.db.jpa.*;
import javax.persistence.*;
import java.util.*;
import play.libs.*;

@Entity
public class User extends JPAModel {
	
	public String email;
	public String passwordHash;
	public String name;
	public String needConfirmation;
	
	// ~~~~~~~~~~~~ 
	
	public User(String email, String password, String name) {
		this.email = email;
		this.passwordHash = Codec.hexMD5(password);
		this.name = name;
		this.needConfirmation = Codec.UUID();
		save();
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
		return Post.find("postedBy = ? order by postedAt", this).page(1, 10);
	}
	
	public Long getPostsCount() {
		return Post.count("postedBy", this);
	}
	
	public Long getTopicsCount() {
		return Post.count("select count(distinct t) from Topic t, Post p, User u where p.postedBy = ? and p.topic = t", this);
	}
	
	// ~~~~~~~~~~~~ 
	
	public static User findByEmail(String email) {
		return find("email", email).one();
	}
	
	public static User findByRegistrationUUID(String uuid) {
		return find("needConfirmation", uuid).one();
	}
	
	public static List<User> findAll(int page, int pageSize) {
		return User.find().page(page, pageSize);
	}
	
	public static boolean isEmailAvailable(String email) {
		return findByEmail(email) == null;
	}

}

