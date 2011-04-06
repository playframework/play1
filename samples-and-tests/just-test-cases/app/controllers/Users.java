package controllers;

import models.*;
import play.*;
import play.data.binding.As;
import play.mvc.*;
import java.text.*;
import java.util.*;

public class Users extends Controller {

    public static void index() {
        render();
    }
    
    public static void list() {
        List users = User.findAll();
        renderText(users.size());
    }

    public static void submit(User u) {
        Logger.info("user date [" + u.birth + "]");
        render(u);
       
    }
    
    public static void changeColor(Factory.Color color, String name) {
        renderText(color + "," + name + ". Errors:" + validation.hasErrors());
    }
    
    public static void changeColors(List<Factory.Color> colors) {
        renderText(colors + ". Errors:" + validation.hasErrors());
    }

    public static void edit() {
		User u = fresh();
		render(u);
	}
	
	public static void save() {
		User u = fresh();
		u.edit("u", params.all());
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
	
	public static void newUser(String name) {
	   User u = new User();
	   u.name = name;
	   u.save();
	   renderText("Created user with name %s", u.name);
	}
    
}

