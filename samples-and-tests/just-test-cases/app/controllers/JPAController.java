package controllers;

import play.*;
import play.mvc.*;
import play.db.jpa.JPA;

import java.util.*;

import models.*;


public class JPAController extends Controller {
	
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

