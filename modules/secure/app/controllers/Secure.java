package controllers;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import play.Play;
import play.mvc.*;
import play.data.validation.*;
import play.libs.*;

public class Secure extends Controller {

    @Before(unless={"login", "authenticate", "logout"})
    static void checkAccess() throws Throwable {
        // Authent
        if(!session.contains("username")) {
            flash.put("url", request.method == "GET" ? request.url : "/"); // seems a good default
            login();
        }
        // Checks
        Check check = getActionAnnotation(Check.class);
        if(check != null) {
            check(check);
        }
        check = getControllerAnnotation(Check.class);
        if(check != null) {
            check(check);
        }
    }

    private static void check(Check check) throws Throwable {
        for(String profile : check.value()) {
            boolean hasProfile = (Boolean)Security.invoke("check", check.value());
            if(!hasProfile && check.type() == Check.Type.AND) {
                Security.invoke("checkFailed", check.value());
            }
            if(hasProfile && check.type() == Check.Type.OR) {
                return;
            }
        }
        Security.invoke("checkFailed", check.value());
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

    public static void authenticate(@Required String username, String password, boolean remember) throws Throwable {
        // Check tokens
        if(validation.hasErrors() || !(Boolean)Security.invoke("authentify", username, password)) {
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

    public static class Security extends Controller {

        static boolean authentify(String username, String password) {
            return true;
        }

        static boolean check(String profile) {
            return true;
        }

        static void checkFailed(String profile) {
            forbidden();
        }

        static String connected() {
            return session.get("username");
        }

        private static Object invoke(String m, Object... args) throws Throwable {
            Class security = null;
            List<Class> classes = Play.classloader.getAssignableClasses(Security.class);
            if(classes.size() == 0) {
                security = Security.class;
            } else {
                security = classes.get(0);
            }
            try {
                return Java.invokeStaticOrParent(security, m, args);
            } catch(InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

    }

}