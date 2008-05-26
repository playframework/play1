package controllers;

import play.mvc.Controller;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;

public class Orders extends Controller {
    
    public static void show(Long id, String name) {
        render(id, name);
    }
    
}