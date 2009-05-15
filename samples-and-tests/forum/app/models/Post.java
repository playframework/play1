package models;

import play.*;
import play.db.jpa.*;
import javax.persistence.*;
import java.util.*;

@Entity
public class Post extends JPAModel {
	
	public String content;
	public Date postedAt;
	@OneToOne public User postedBy;	
	@ManyToOne public Topic topic;
	
	// ~~~~~~~~~~~~ 
	
	public Post(Topic topic, User postedBy, String content) {
		this.topic = topic;
		this.postedBy = postedBy;
		this.content = content;
		this.postedAt = new Date();
		save();
	}
	
}

