package controllers;

import play.mvc.Controller;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;

public class Orders extends Controller {
	
	@play.mvc.Before
	static void check() {
		int b = 9/1;		
	}
    
	public static void show(Long id, String name) {
		render(id, name);
	}
    
}