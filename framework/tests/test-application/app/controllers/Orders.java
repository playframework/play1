package controllers;

import play.mvc.Controller;

public class Orders extends Controller {
    
    public static void show(Long id) {
        renderText("This is the order %s", id);
    }
    
}