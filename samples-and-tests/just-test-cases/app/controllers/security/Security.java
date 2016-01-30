package controllers.security;

import controllers.Secure;

public class Security extends Secure.Security {

    public static void login() {
        render();
    }

    static boolean authenticate(String username, String password) {
        return false;
    }

}
