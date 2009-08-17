package controllers;

import play.mvc.*;
import play.modules.gae.*;

import com.google.appengine.api.users.*;

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