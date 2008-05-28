package controllers;

import play.mvc.Controller;
import play.db.DB;
import java.sql.ResultSet;

public class Hello extends Controller {
	
	public static void hello() throws Exception {
		DB.execute("INSERT INTO toto VALUES('Guillaume')");
		ResultSet result = DB.executeQuery("SELECT count(*) from toto");
		result.next();
		Integer count = result.getInt(1);
		render(count);
	}
	
}