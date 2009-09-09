package controllers;

import models.*;

public class Security extends Secure.Security {

    static boolean authentify(String username, String password) {
        return User.connect(username, password) != null;
    }
    
    static boolean check(String profile) {
        if("isAdmin".equals(profile)) {
            return User.find("byEmail", connected()).<User>first().isAdmin;
        }
        return false;
    }
    
}

