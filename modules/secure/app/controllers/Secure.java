package controllers;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import play.Play;
import play.mvc.*;
import play.data.validation.*;
import play.libs.*;
import play.utils.*;

public class Secure extends Controller {

    @Before(unless={"login", "authenticate", "logout"})
    static void checkAccess() throws Throwable {
        // Authent
        if(!session.contains("username")) {
            flash.put("url", "GET".equals(request.method) ? request.url : "/"); // seems a good default
            login();
        }
        // Checks
        Check check = getActionAnnotation(Check.class);
        if(check != null) {
            check(check);
        }
        check = getControllerInheritedAnnotation(Check.class);
        if(check != null) {
            check(check);
        }
    }

    private static void check(Check check) throws Throwable {
        for(String profile : check.value()) {
            boolean hasProfile = (Boolean)Security.invoke("check", profile);
            if(!hasProfile) {
                Security.invoke("onCheckFailed", profile);
            }
        }
    }

    // ~~~ Login

    public static void login() throws Throwable {
        Http.Cookie remember = request.cookies.get("rememberme");
        if(remember != null && remember.value.indexOf("-") > 0) {
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
        Boolean allowed = false;
        try {
            // This is the deprecated method name
            allowed = (Boolean)Security.invoke("authentify", username, password);
        } catch (UnsupportedOperationException e ) {
            // This is the official method name
            allowed = (Boolean)Security.invoke("authenticate", username, password);
        }
        if(validation.hasErrors() || !allowed) {
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

    public static void logout() throws Throwable {
        Security.invoke("onDisconnect");
        session.clear();
        response.removeCookie("rememberme");
        Security.invoke("onDisconnected");
        flash.success("secure.logout");
        login();
    }

    // ~~~ Utils

    static void redirectToOriginalURL() throws Throwable {
        Security.invoke("onAuthenticated");
        String url = flash.get("url");
        if(url == null) {
            url = "/";
        }
        redirect(url);
    }

    public static class Security extends Controller {

        /**
         * @Deprecated
         * 
         * @param username
         * @param password
         * @return
         */
        static boolean authentify(String username, String password) {
            throw new UnsupportedOperationException();
        }

        /**
         * This method is called during the authentication process. This is where you check if
         * the user is allowed to log in into the system. This is the actual authentication process
         * against a third party system (most of the time a DB).
         *
         * @param username
         * @param password
         * @return true if the authentication process succeeded
         */
        static boolean authenticate(String username, String password) {
            return true;
        }

        /**
         * This method checks that a profile is allowed to view this page/method. This method is called prior
         * to the method's controller annotated with the @Check method. 
         *
         * @param profile
         * @return true if you are allowed to execute this controller method.
         */
        static boolean check(String profile) {
            return true;
        }

        /**
         * This method returns the current connected username
         * @return
         */
        static String connected() {
            return session.get("username");
        }

        /**
         * Indicate if a user is currently connected
         * @return  true if the user is connected
         */
        static boolean isConnected() {
            return session.contains("username");
        }

        /**
         * This method is called after a successful authentication.
         * You need to override this method if you with to perform specific actions (eg. Record the time the user signed in)
         */
        static void onAuthenticated() {
        }

         /**
         * This method is called before a user tries to sign off.
         * You need to override this method if you wish to perform specific actions (eg. Record the name of the user who signed off)
         */
        static void onDisconnect() {
        }

         /**
         * This method is called after a successful sign off.
         * You need to override this method if you wish to perform specific actions (eg. Record the time the user signed off)
         */
        static void onDisconnected() {
        }

        /**
         * This method is called if a check does not succeed. By default it shows the not allowed page (the controller forbidden method).
         * @param profile
         */
        static void onCheckFailed(String profile) {
            forbidden();
        }

        private static Object invoke(String m, Object... args) throws Throwable {

            try {
                return Java.invokeChildOrStatic(Security.class, m, args);       
            } catch(InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

    }

}
