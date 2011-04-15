package play.libs;

import java.util.HashMap;
import java.util.Map;

import play.mvc.Http.Request;
import play.mvc.Scope.Params;
import play.mvc.results.Redirect;

/**
 * Library to access ressources protected by OAuth 2.0. For OAuth 1.0a, see play.libs.OAuth.
 * See the facebook-oauth2 example for usage.
 *
 */
public class OAuth2 {

    public String authorizationURL;
    public String accessTokenURL;
    public String clientid;
    public String secret;

    public OAuth2(String authorizationURL,
            String accessTokenURL,
            String clientid,
            String secret) {
        this.accessTokenURL = accessTokenURL;
        this.authorizationURL = authorizationURL;
        this.clientid = clientid;
        this.secret = secret;
    }

    public static boolean isCodeResponse() {
        return Params.current().get("code") != null;
    }

    public void requestAccessToken() {
        String callbackURL = Request.current().getBase() + Request.current().url;
        throw new Redirect(accessTokenURL
                + "?client_id=" + clientid
                + "&redirect_uri=" + callbackURL);
    }

    public String getAccessToken() {
        String callbackURL = Request.current().getBase() + Request.current().url;
        String accessCode = Params.current().get("code");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("client_id", clientid);
        params.put("client_secret", secret);
        params.put("redirect_uri", callbackURL);
        params.put("code", accessCode);
        Map<String, String> response = WS.url(authorizationURL).params(params).get().getQueryString();
        return response.get("access_token");
    }

}
