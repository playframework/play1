package controllers;

import play.*;
import play.mvc.*;
import play.libs.*;
import java.util.*;
import models.*;
import notifiers.*;
import play.data.validation.*;

public class Application extends Controller {
	
	static Integer pageSize = Integer.parseInt(Play.configuration.getProperty("forum.pageSize", "10"));
	
	// ~~~~~~~~~~~~ @Before interceptors
	
	@Before
	static void globals() {
		renderArgs.put("connected", connectedUser());
		renderArgs.put("pageSize", pageSize);
	}
	
	@Before
	static void checkSecure() {
		Secure secure = getActionAnnotation(Secure.class);
		if(secure != null) {
			if(connectedUser() == null || (secure.admin() && !connectedUser().isAdmin())) {
				forbidden();
			}
		}
	}
	
	// ~~~~~~~~~~~~ Actions
	
	public static void signup() {
		render();
	}
	
	public static void register(@Required @Email String email, @Required @MinSize(5) String password, @Equals("password") String password2, @Required String name) {
		if(validation.hasErrors()) {
			validation.keep();
			params.flash();
			flash.error("Please correct these errors !");
			signup();
		}
		User user = new User(email, password, name);
		if(Notifier.welcome(user)) {
			flash.success("Your account is created. Please check your emails ...");
			login();
		} 
		flash.error("Oops ... (the email cannot be sent)");
		login();
	}
	
	public static void confirmRegistration(String uuid) {
		User user = User.findByRegistrationUUID(uuid);
		notFoundIfNull(user);
		user.needConfirmation = null;
		user.save();
		connect(user);
		flash.success("Welcome %s !", user.name);
		Users.show(user.id);
	}
	
	public static void login() {
		render();
	}
	
	public static void authenticate(String email, String password) {
		User user = User.findByEmail(email);
		if(user == null || !user.checkPassword(password)) {
			flash.error("Bad email or bad password");
			flash.put("email", email);
			login();
		} else if(user.needConfirmation != null) {
			flash.error("This account is not confirmed");
			flash.put("notconfirmed", user.needConfirmation);
			flash.put("email", email);
			login();
		}
		connect(user);
		flash.success("Welcome back %s !", user.name);
		Users.show(user.id);
	}
	
	public static void logout() {
		flash.success("You've been logged out");
		session.clear();
		Forums.index();
	}
	
	public static void resendConfirmation(String uuid) {
		User user = User.findByRegistrationUUID(uuid);
		notFoundIfNull(user);
		if(Notifier.welcome(user)) {
			flash.success("Please check your emails ...");
			flash.put("email", user.email);
			login();
		} 
		flash.error("Oops (the email cannot be sent)...");
		flash.put("email", user.email);
		login();
	}
	
	// ~~~~~~~~~~~~ Some utils
	
	static void connect(User user) {
		session.put("logged", user.id);
	}
	
	static User connectedUser() {
		String userId = session.get("logged");
		return userId == null ? null : (User)User.findById(Long.parseLong(userId));
	}
    
}
