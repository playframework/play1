package controllers;

import play.*;
import play.mvc.*;
import play.test.Fixtures;

import java.io.File;
import java.util.*;

import models.*;
import play.vfs.VirtualFile;

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

     public static void upload(File file) {
        
        Http.Response.current().headers.put("Content-Length", new Http.Header("Content-Length", String.valueOf(file.length())));
        renderBinary(file);
    }
}
