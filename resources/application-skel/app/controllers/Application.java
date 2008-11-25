package controllers;

import play.*;
import play.mvc.*;
import java.util.*;

public class Application extends Controller {

	/**
	 * Default action.
	 * renders the app/views/Application/index.html template
	 */
	public static void index() {
		render();
	}
    
}
