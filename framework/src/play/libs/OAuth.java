package play.libs;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import play.mvc.Http.Request;
import play.mvc.Scope.Params;

public class OAuth {

    private OAuthConsumer consumer;
    private OAuthProvider provider;

    private OAuth(ServiceInfo info) {
        consumer = new DefaultOAuthConsumer(info.consumerKey, info.consumerSecret);
        provider = new DefaultOAuthProvider(info.requestTokenURL, info.accessTokenURL, info.authorizationURL);
        provider.setOAuth10a(true);
    }

    /**
     * Create an OAuth object for the service described in info
     * @param info must contain all informations related to the service
     * @return the OAuth object
     */
    public static OAuth service(ServiceInfo info) {
        return new OAuth(info);
    }

    /**
     * Set the token and secret of the current user
     * @param tokens the TokenPair
     * @return the OAuth object for chaining
     */
    public OAuth tokens(TokenPair tokens) {
        consumer.setTokenWithSecret(tokens.token, tokens.secret);
        return this;
    }

    /**
     * Set the token and secret of the current user
     * @return the OAuth object for chaining
     */
    public OAuth tokens(String token, String secret) {
        consumer.setTokenWithSecret(token, secret);
        return this;
    }

    /**
     * Return the token and secret stored - this is usefull after calling requestUnauthorizedToken
     * because the value will be updated with information we got from the server
     * @return the TokenPair
     */
    public TokenPair getTokens() {
        return new TokenPair(consumer.getToken(), consumer.getTokenSecret());
    }

    public static boolean isVerifierResponse() {
        return Params.current().get("oauth_verifier") != null;
    }

    public static String getResponseToken() {
        return Params.current().get("oauth_token");
    }

    public static String getResponseSecret() {
        return Params.current().get("oauth_token_secret");
    }

    /**
     * Request the unauthorized token and secret. They can then be read with getTokens()
     * @return the url to redirect the user to get the verifier and continue the process
     */
    public String requestUnauthorizedToken() {
        String callbackURL = Request.current().getBase() + Request.current().url;
        try {
            return provider.retrieveRequestToken(consumer, callbackURL);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public TokenPair requestAccessToken() {
        String verifier = Params.current().get("oauth_verifier");
        try {
            provider.retrieveAccessToken(consumer, verifier);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new TokenPair(consumer.getToken(), consumer.getTokenSecret());
    }

    public static class ServiceInfo {
        public String requestTokenURL;
        public String accessTokenURL;
        public String authorizationURL;
        public String consumerKey;
        public String consumerSecret;
        public ServiceInfo(String requestTokenURL,
                            String accessTokenURL,
                            String authorizationURL,
                            String consumerKey,
                            String consumerSecret) {
            this.requestTokenURL = requestTokenURL;
            this.accessTokenURL = accessTokenURL;
            this.authorizationURL = authorizationURL;
            this.consumerKey = consumerKey;
            this.consumerSecret = consumerSecret;
        }
    }

    public static class TokenPair {
        public String token;
        public String secret;
        public TokenPair(String token, String secret) {
            this.token = token;
            this.secret = secret;
        }
        @Override
        public String toString() {
            return token + " - " + secret;
        }
    }

}
