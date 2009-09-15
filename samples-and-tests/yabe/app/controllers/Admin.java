package controllers;
 
import play.*;
import play.mvc.*;
import play.data.validation.*;
 
import java.util.*;
 
import models.*;
 
@With(Secure.class)
public class Admin extends Controller {
    
    @Before
    static void setConnectedUser() {
        if(Security.isConnected()) {
            User user = User.find("byEmail", Security.connected()).first();
            renderArgs.put("user", user.fullname);
        }
    }
 
    public static void index() {
        List<Post> posts = Post.find("author.email", Security.connected()).fetch();
        render(posts);
    }
    
    public static void form(Long id) {
        if(id != null) {
            Post post = Post.findById(id);
            render(post);
        }
        render();
    }
    
    public static void save(Long id, Post post, String tags) {
        // Edition ?
        if(id != null) {
            post = Post.findById(id);
            post.edit("post", params);
        }
        // Set new tags list
        Set<Tag> newTags = new TreeSet<Tag>();
        for(String tag : tags.split("\\s+")) {
            if(tag.trim().length() > 0) {
                newTags.add(Tag.findOrCreateByName(tag));
            }
        }
        post.tags = newTags;
        // And other stuff
        post.author = User.find("byEmail", Security.connected()).first();
        post.postedAt = new Date();
        // Validate
        validation.valid(post);
        if(validation.hasErrors()) {
            render("@form", post);
        }
        // Save
        post.save();
        index();
    }
    
}
