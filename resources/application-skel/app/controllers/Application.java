package controllers;

import java.util.*;

import play.*;
import play.mvc.*;
import play.libs.*;

public class Application extends Controller {

	/**
	 * Change render("welcome.html"); to render();
	 * and it will display the /app/views/Application/index.html template
	 */
	public static void index() {
		render("welcome.html");
	}

}