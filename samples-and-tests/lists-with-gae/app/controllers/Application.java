package controllers;

import play.mvc.*;
import play.modules.gae.*;


public class Application extends Controller {

    public static void index() {
        if(GAE.isLoggedIn()) {
            Lists.index();
        }
        render();
    }
    
    public static void login() {
        GAE.login("Application.index");
    }
    
    public static void logout() {
        GAE.logout("Application.index");
    }

}