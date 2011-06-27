package controllers;

import java.net.URLEncoder;

import play.Logger;
import play.libs.OAuth;
import play.libs.OAuth.ServiceInfo;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.mvc.Controller;
import play.mvc.Scope.Params;

import models.User;

public class Application extends Controller {

    private static final ServiceInfo TWITTER = new ServiceInfo(
            "http://twitter.com/oauth/request_token",
            "http://twitter.com/oauth/access_token",
            "http://twitter.com/oauth/authorize",
            "eevIR82fiFK3e6VrGpO9rw",
            "OYCQA6fpsLiMVaxqqm1EqDjDWFmdlbkSYYcIbwICrg"
    );

    public static void index() {
        String url = "http://twitter.com/statuses/mentions.xml";
        String mentions = "";
        mentions = WS.url(url).oauth(TWITTER, getUser().token, getUser().secret).get().getString();
        render(mentions);
    }

    public static void setStatus(String status) throws Exception {
        String url = "http://twitter.com/statuses/update.json?status=" + URLEncoder.encode(status, "utf-8");
        String response = WS.url(url).oauth(TWITTER, getUser().token, getUser().secret).post().getString();
        request.current().contentType = "application/json";
        renderText(response);
    }

    // Twitter authentication

    public static void authenticate() {
        User user = getUser();
        if (OAuth.isVerifierResponse()) {
            // We got the verifier; now get the access token, store it and back to index
            OAuth.Response oauthResponse = OAuth.service(TWITTER).retrieveAccessToken(user.token, user.secret);
            if (oauthResponse.error == null) {
                user.token = oauthResponse.token;
                user.secret = oauthResponse.secret;
                user.save();
            } else {
                Logger.error("Error connecting to twitter: " + oauthResponse.error);
            }
            index();
        }
        OAuth twitt = OAuth.service(TWITTER);
        OAuth.Response oauthResponse = twitt.retrieveRequestToken();
        if (oauthResponse.error == null) {
            // We received the unauthorized tokens in the OAuth object - store it before we proceed
            user.token = oauthResponse.token;
            user.secret = oauthResponse.secret;
            user.save();
            redirect(twitt.redirectUrl(oauthResponse.token));
        } else {
            Logger.error("Error connecting to twitter: " + oauthResponse.error);
            index();
        }
    }

    private static User getUser() {
        return User.findOrCreate("guest");
    }

}
