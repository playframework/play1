package play.libs;

import java.util.HashMap;
import java.util.Map;

import play.mvc.Http.Request;
import play.mvc.Scope.Params;
import play.mvc.results.Redirect;

import play.libs.WS.HttpResponse;

import com.google.gson.JsonObject;

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

    /**
     * First step of the OAuth2 process: redirects the user to the authorization page
     *
     * @param callbackURL
     */
    public void retrieveVerificationCode(String callbackURL) {
        throw new Redirect(authorizationURL
                + "?client_id=" + clientid
                + "&redirect_uri=" + callbackURL);
    }

    public void retrieveVerificationCode() {
        retrieveVerificationCode(Request.current().getBase() + Request.current().url);
    }

    public Response retrieveAccessToken(String callbackURL) {
        String accessCode = Params.current().get("code");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("client_id", clientid);
        params.put("client_secret", secret);
        params.put("redirect_uri", callbackURL);
        params.put("code", accessCode);
        HttpResponse response = WS.url(accessTokenURL).params(params).get();
        return new Response(response);
    }

    public Response retrieveAccessToken() {
        return retrieveAccessToken(Request.current().getBase() + Request.current().url);
    }

    /**
     * @deprecated Use @{link play.libs.OAuth2.retrieveVerificationCode()} instead
     */
    @Deprecated
    public void requestAccessToken() {
        retrieveVerificationCode();
    }

    /**
     * @deprecated Use @{link play.libs.OAuth2.retrieveAccessToken()} instead
     */
    @Deprecated
    public String getAccessToken() {
        return retrieveAccessToken().accessToken;
    }

    public static class Response {
        public final String accessToken;
        public final Error error;
        public final WS.HttpResponse httpResponse;
        private Response(String accessToken, Error error, WS.HttpResponse response) {
            this.accessToken = accessToken;
            this.error = error;
            this.httpResponse = response;
        }
        public Response(WS.HttpResponse response) {
            this.httpResponse = response;
            Map<String, String> querystring = response.getQueryString();
            if (querystring.containsKey("access_token")) {
                this.accessToken = querystring.get("access_token");
                this.error = null;
            } else {
                this.accessToken = null;
                this.error = Error.oauth2(response);
            }
        }
        public static Response error(Error error, WS.HttpResponse response) {
            return new Response(null, error, response);
        }
    }

    public static class Error {
        public final Type type;
        public final String error;
        public final String description;
        public enum Type {
            COMMUNICATION,
            OAUTH,
            UNKNOWN
        }
        private Error(Type type, String error, String description) {
            this.type = type;
            this.error = error;
            this.description = description;
        }
        static Error communication() {
            return new Error(Type.COMMUNICATION, null, null);
        }
        static Error oauth2(WS.HttpResponse response) {
            if (response.getQueryString().containsKey("error")) {
                Map<String, String> qs = response.getQueryString();
                return new Error(Type.OAUTH,
                        qs.get("error"),
                        qs.get("error_description"));
            } else if (response.getContentType().startsWith("text/javascript")) { // Stupid Facebook returns JSON with the wrong encoding
                JsonObject jsonResponse = response.getJson().getAsJsonObject().getAsJsonObject("error");
                return new Error(Type.OAUTH,
                        jsonResponse.getAsJsonPrimitive("type").getAsString(),
                        jsonResponse.getAsJsonPrimitive("message").getAsString());
            } else {
                return new Error(Type.UNKNOWN, null, null);
            }
        }
        @Override public String toString() {
            return "OAuth2 Error: " + type + " - " + error + " (" + description + ")";
        }
    }

}
