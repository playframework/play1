package play.libs;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import play.mvc.Http.Request;
import play.mvc.Scope.Params;

/**
 * Library to access ressources protected by OAuth 1.0a. For OAuth 2.0, see play.libs.OAuth2.
 *
 */
public class OAuth {

    private ServiceInfo info;
    private OAuthProvider provider;

    private OAuth(ServiceInfo info) {
        this.info = info;
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

    public static boolean isVerifierResponse() {
        return Params.current().get("oauth_verifier") != null;
    }

    /**
     * Request the unauthorized token and secret. They can then be read with getTokens()
     * @return the url to redirect the user to get the verifier and continue the process
     */
    public TokenPair requestUnauthorizedToken() {
        OAuthConsumer consumer = new DefaultOAuthConsumer(info.consumerKey, info.consumerSecret);
        String callbackURL = Request.current().getBase() + Request.current().url;
        try {
            provider.retrieveRequestToken(consumer, callbackURL);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new TokenPair(consumer.getToken(), consumer.getTokenSecret());
    }

    public TokenPair requestAccessToken(TokenPair tokenPair) {
        OAuthConsumer consumer = new DefaultOAuthConsumer(info.consumerKey, info.consumerSecret);
        consumer.setTokenWithSecret(tokenPair.token, tokenPair.secret);
        String verifier = Params.current().get("oauth_verifier");
        try {
            provider.retrieveAccessToken(consumer, verifier);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new TokenPair(consumer.getToken(), consumer.getTokenSecret());
    }

    public String redirectUrl(TokenPair tokenPair) {
        return oauth.signpost.OAuth.addQueryParameters(provider.getAuthorizationWebsiteUrl(),
                oauth.signpost.OAuth.OAUTH_TOKEN, tokenPair.token);
    }

    /**
     * Information relative to an OAuth 1.0 provider.
     *
     */
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
