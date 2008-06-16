package controllers;

import play.mvc.Controller;
import play.db.DB;
import java.sql.ResultSet;

import models.Client;
import java.util.List;

public class Hello extends Controller {
	
	public static void hello_() throws Exception {
		DB.execute("INSERT INTO toto VALUES('Guillaume')");
		ResultSet result = DB.executeQuery("SELECT count(*) from toto");
		result.next();
		Integer count = result.getInt(1);
		render(count);
	}
	
	public static void hello() {
		Client jojo = new Client();
		jojo.name = "Jojo";
		jojo.age = 45;
		jojo.save();
		List clients = Client.findBy("age = ?", 45);
		render(clients);
	}
	
}
