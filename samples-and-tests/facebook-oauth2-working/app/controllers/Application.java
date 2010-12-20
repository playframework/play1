package controllers;

import java.util.HashMap;
import java.util.Map;

import models.User;
import play.Logger;
import play.libs.WS;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Router;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.mail.handlers.message_rfc822;

public class Application extends Controller {

    public static String CLIENTID = "95341411595";
    public static String SECRET = "8eff1b488da7fe3426f9ecaf8de1ba54";

    @Before
    static void auth() {
        User user;
        if (session.contains("uid")) {
            Logger.info("existing user: " + session.get("uid"));
            user = User.get(Long.parseLong(session.get("uid")));
        } else {
            user = User.createNew();
            session.put("uid", user.uid);
        }
        renderArgs.put("user", user);
    }

    static User connected() {
        return (User)renderArgs.get("user");
    }

    public static void index() {
        User u = connected();
        JsonObject me = null;
        if (u != null && u.access_token != null) {
            me = WS.url("https://graph.facebook.com/me?access_token=%s", u.access_token).get().getJson().getAsJsonObject();
        }
        render(me);
    }

    public static void auth(String code) {
        Logger.info("Got code: " + code);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("client_id", CLIENTID);
        params.put("redirect_uri", Router.getFullUrl("Application.auth"));
        params.put("client_secret", SECRET);
        params.put("code", code);
        Map<String, String> response = WS.url("https://graph.facebook.com/oauth/access_token").params(params).get().getQueryString();
        Logger.info("Size: " + response.size());
        // renderText("Access code: " + response.get("access_token"));
        Logger.info("Access token: " + response.get("access_token"));
        User u = connected();
        u.access_token = response.get("access_token");
        u.save();
        index();
    }

    public static void login() {
        String callback = Router.getFullUrl("Application.auth");
        redirect("https://graph.facebook.com/oauth/authorize?client_id=" + CLIENTID + "&redirect_uri=" + callback);
    }

}
