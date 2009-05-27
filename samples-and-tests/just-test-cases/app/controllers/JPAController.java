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
		JPA.abort();
		list();
	}
	
	public static void list() {
		List users = User.findAll();
		render(users);
	}
    
}

