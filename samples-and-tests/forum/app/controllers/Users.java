package controllers;

import models.*;
import play.*;
import play.mvc.*;
import java.util.*;

public class Users extends Application {

	public static void index(Integer page) {
		List users = User.findAll(page == null ? 1 : page, pageSize);
		Long nbUsers = User.count();
		render(nbUsers, users, page);
	}
	
	public static void show(Long id) {
		User user = User.findById(id);
		notFoundIfNull(user); 
		render(user);
	}
    
}

