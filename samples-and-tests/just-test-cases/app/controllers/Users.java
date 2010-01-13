package controllers;

import models.*;
import play.*;
import play.data.binding.annotations.As;
import play.mvc.*;
import java.text.*;

public class Users extends Controller {

    public static void index() {
        render();
    }

    public static void submit(User u) {
        Logger.info("user date [" + u.birth + "]");
        render(u);
       
    }

    	public static void edit() {
		User u = fresh();
		render(u);
	}
	
	public static void save() {
		User u = fresh();
		u.edit("u", params);
		render(u);
	}
		
	static User fresh() {
		try {
			User u = new User();
			u.name = "Guillaume";
			u.b = true;
			u.l = 356L;
			u.birth = new SimpleDateFormat("dd/MM/yyyy").parse("21/12/1980");
			return u;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void wbyte(byte a, Byte b) {
	    renderText(a+","+b);
	}
    
}

