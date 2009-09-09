package controllers;

import play.*;
import play.mvc.*;
import play.db.jpa.JPA;

import java.util.*;

import models.*;


public class JPAController extends Controller {
    
    public static void init() {
        Post post = new Post();
        post.name = "Post1";
        Tag tag1 = new Tag();
        tag1.name = "Blue";
        Tag tag2 = new Tag();
        tag2.name = "Red";
        post.tags.add(tag1);
        post.tags.add(tag2);
        post.save();
        show();
    }
    
    public static void show() {
        Post post = Post.all().first();
        render(post);
    }
    
    public static void dontEditName() {
        Post post = Post.all().first();
        post.name = "Kiki";
        show();
    }
    
    public static void editName() {
        Post post = Post.all().first();
        post.name = "Kiki";
        post.save();
        show();
    }
    
    public static void dontEditTags() {
        Post post = Post.all().first();
        post.tags.remove(0);
        show();
    }
    
    public static void editTags() {
        Post post = Post.all().first();
        post.tags.remove(0);
        post.save();
        show();
    }
	
	public static void index() {
		render();
	}

	public static void willSave(String name) {
		User u = new User(name);
		u.save();
		list();
	}
	
	public static void willNotSave(String name) {
		User u = new User(name);
		u.save();
		JPA.setRollbackOnly();
		list();
	}
	
	public static void list() {
		List users = User.findAll();
		render(users);
	}
	
	public static void edit(String name, String newName) {
	    User u = User.find("byName", name).one();
	    u.name = newName;
	    list();
	}
	
	public static void editAndSave(String name, String newName) {
	    User u = User.find("byName", name).one();
	    u.name = newName;
	    u.save();
	    list();
	}
    
    public static void createAndEdit(String name, String newName) {
        User u = new User(name);
		u.save();
		u.name = newName;
		list(); 
    }
    
    public static void createAndEditAndSave(String name, String newName) {
        User u = new User(name);
		u.save();
		u.name = newName;
		u.save();
		list(); 
    }
}

