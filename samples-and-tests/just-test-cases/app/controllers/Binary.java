package controllers;

import play.*;
import play.mvc.*;
import play.test.Fixtures;

import java.util.*;

import models.*;

public class Binary extends Controller {
	
	public static void deleteAll(){ // see Bug #491403
		Fixtures.deleteAll();
	}

	public static void index() {
		render();
	}

	public static void save(UserWithAvatar user) {
		user.save();
		show(user.id);
	}

	public static void show(Long id) {
		UserWithAvatar user = UserWithAvatar.findById(id);
		render(user);
	}

	public static void showAvatar(Long id) {
		UserWithAvatar user = UserWithAvatar.findById(id);
		if (user != null && user.avatar.exists()) {
			renderBinary(user.avatar.get());
		}
		notFound();
	}

}
