package controllers;

import play.mvc.*;

public class GAEActions extends Controller {

    public static void login() {
        String url = params.get("continue");
        render(url);
    }

    public static void doLogin(String email, String url, boolean isAdmin) {
        if(email!= null && !email.trim().equals("")) {
            session.put("__GAE_EMAIL", email);
            session.put("__GAE_ISADMIN", isAdmin);
        }
        redirect(url);
    }

    public static void logout() {
        String url = params.get("continue");
        session.clear();
        redirect(url);
    }

}