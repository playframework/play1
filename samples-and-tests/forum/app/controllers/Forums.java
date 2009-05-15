package controllers;

import models.*;
import play.*;
import play.mvc.*;
import java.util.*;

public class Forums extends Application {

	public static void index() {
		List forums = Forum.findAll();	
		long topicsCount = Topic.count();
		long postsCount = Post.count();
		render(forums, topicsCount, postsCount);
	}
    
	@Secure(admin=true)
	public static void create(String name, String description) {
		if(name.trim().length() == 0) {
			flash.put("description", description);
			flash.error("Please give a name for the new forum");
			index();
		}
		Forum forum = new Forum(name, description);
		index();
	}
	
	public static void show(Long forumId, Integer page) {
		Forum forum = Forum.findById(forumId);
		notFoundIfNull(forum);
		render(forum, page);
	}
	
	@Secure(admin=true)
	public static void delete(Long forumId) {
		Forum forum = Forum.findById(forumId);
		notFoundIfNull(forum);
		forum.delete(); 
		flash("success", "The forum has been deleted");
		index();
	}

}

