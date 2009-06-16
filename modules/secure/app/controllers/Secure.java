package controllers;

import play.mvc.*;
import play.data.validation.*;
import play.libs.*;

public class Secure extends Controller {

    @Before(unless={"login", "authenticate", "logout"})
    static void checkAccess() {
        if(!session.contains("username")) {
            flash.put("url", request.method == "GET" ? request.url : "/"); // seems a good default
            login();
        }
    }
    
    // ~~~ Login

    public static void login() {
        Http.Cookie remember = request.cookies.get("rememberme");
        if(remember != null) {
            String sign = remember.value.substring(0, remember.value.indexOf("-"));
            String username = remember.value.substring(remember.value.indexOf("-") + 1);
            if(Crypto.sign(username).equals(sign)) {
                session.put("username", username);
                redirectToOriginalURL();
            }
        }
        flash.keep("url");
        render();
    }

    public static void authenticate(@Required String username, String password, boolean remember) {
        // Check tokens
        if(validation.hasErrors()) {
            flash.keep("url");
            flash.error("secure.error");
            params.flash();
            login();
        }
        // Mark user as connected
        session.put("username", username);
        // Remember if needed
        if(remember) {
            response.setCookie("rememberme", Crypto.sign(username) + "-" + username, "30d");
        }
        // Redirect to the original URL (or /)
        redirectToOriginalURL();
    }

    public static void logout() {
        session.clear();
        response.setCookie("rememberme", "", 0);
        flash.success("secure.logout");
        login();
    }
    
    // ~~~ Utils
    
    static void redirectToOriginalURL() {
        String url = flash.get("url");
        if(url == null) {
            url = "/";
        }
        redirect(url);
    }

}