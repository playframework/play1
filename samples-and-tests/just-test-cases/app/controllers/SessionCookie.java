package controllers;

import play.*;
import play.mvc.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import models.*;

public class SessionCookie extends Controller {

	public static void index() {
        render();
    }

	public static void put() {
        session.put("msg", "Yop");
        render("@index");
    }

	public static void remove() {
        session.remove("msg");
        render("@index");
    }

	public static void changeMaxAgeConstant(String maxAge) {
    	try {
	    	/*
	    	 * Set the final static value Scope.COOKIE_EXPIRE using reflection.
	    	 */
	        Field field = Scope.class.getField("COOKIE_EXPIRE");
	        field.setAccessible(true);
	        Field modifiersField = Field.class.getDeclaredField("modifiers");
	        modifiersField.setAccessible(true);
	        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

	        // Set the new value
	        field.set(null, maxAge);
    	} catch(Exception e) {
    		throw new RuntimeException(e);
    	}
    }
}

