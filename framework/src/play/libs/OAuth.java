package play.libs;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
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
     * Request the request token and secret.
     * @return a Response object holding either the result in case of a success or the error
     */
    public Response retrieveRequestToken() {
        return retrieveRequestToken(Request.current().getBase() + Request.current().url);
    }

    /**
     * Request the request token and secret.
     * @param callbackURL the URL where the provider should redirect to
     * @return a Response object holding either the result in case of a success or the error
     */
    public Response retrieveRequestToken(String callbackURL) {
        OAuthConsumer consumer = new DefaultOAuthConsumer(info.consumerKey, info.consumerSecret);
        try {
            provider.retrieveRequestToken(consumer, callbackURL);
        } catch (OAuthException e) {
            return Response.error(new Error(e));
        }
        return Response.success(consumer.getToken(), consumer.getTokenSecret());
    }

    /**
     * Exchange a request token for an access token.
     * @param requestTokenResponse a successful response obtained from retrieveRequestToken
     * @return a Response object holding either the result in case of a success or the error
     */
    public Response retrieveAccessToken(Response requestTokenResponse) {
        return retrieveAccessToken(requestTokenResponse.token, requestTokenResponse.secret);
    }

    /**
     * Exchange a request token for an access token.
     * @param token the token obtained from a previous call
     * @param secret your application secret
     * @return a Response object holding either the result in case of a success or the error
     */
    public Response retrieveAccessToken(String token, String secret) {
         OAuthConsumer consumer = new DefaultOAuthConsumer(info.consumerKey, info.consumerSecret);
        consumer.setTokenWithSecret(token, secret);
        String verifier = Params.current().get("oauth_verifier");
        try {
            provider.retrieveAccessToken(consumer, verifier);
        } catch (OAuthException e) {
            return Response.error(new Error(e));
        }
        return Response.success(consumer.getToken(), consumer.getTokenSecret());
    }

    /**
     * Request the unauthorized token and secret. They can then be read with getTokens()
     * @return the url to redirect the user to get the verifier and continue the process
     * @deprecated use retrieveRequestToken() instead
     */
    @Deprecated
    public TokenPair requestUnauthorizedToken() {
        Response response = retrieveRequestToken();
        return new TokenPair(response.token, response.secret);
    }

    /**
     * @deprecated use retrieveAccessToken() instead
     */
    @Deprecated
    public TokenPair requestAccessToken(TokenPair tokenPair) {
        Response response = retrieveAccessToken(tokenPair.token, tokenPair.secret);
        return new TokenPair(response.token, response.secret);
    }

    public String redirectUrl(String token) {
        return oauth.signpost.OAuth.addQueryParameters(provider.getAuthorizationWebsiteUrl(),
                oauth.signpost.OAuth.OAUTH_TOKEN, token);
    }

    @Deprecated
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

    /**
     * Response to an OAuth 1.0 request.
     *     If success token and secret are non null, and error is null.
     *     If error token and secret are null, and error is non null.
     *
     */
    public static class Response {
        public final String token;
        public final String secret;
        public final Error error;
        private Response(String token, String secret, Error error) {
            this.token = token;
            this.secret = secret;
            this.error = error;
        }
        /**
         * Create a new success response
         * @param pair the TokenPair returned by the provider
         * @return a new Response object holding the token pair
         */
        private static Response success(String token, String secret) {
            return new Response(token, secret, null);
        }
        private static Response error(Error error) {
            return new Response(null, null, error);
        }
        @Override public String toString() {
            return (error != null) ? ("Error: " + error)
                                    : ("Success: " + token + " - " + secret);
        }
    }

    public static class Error {
        public final OAuthException exception;
        public final Type type;
        public final String details;
        public enum Type {
            MESSAGE_SIGNER,
            NOT_AUTHORIZED,
            EXPECTATION_FAILED,
            COMMUNICATION,
            OTHER
        }
        private Error(OAuthException exception) {
            this.exception = exception;
            if (this.exception instanceof OAuthMessageSignerException) {
                this.type = Type.MESSAGE_SIGNER;
            } else if (this.exception instanceof OAuthNotAuthorizedException) {
                this.type = Type.NOT_AUTHORIZED;
            } else if (this.exception instanceof OAuthExpectationFailedException) {
                this.type = Type.EXPECTATION_FAILED;
            } else if (this.exception instanceof OAuthCommunicationException) {
                this.type = Type.COMMUNICATION;
            } else {
                this.type = Type.OTHER;
            }
            this.details = exception.getMessage();
        }
        public String details() { return details; }
        @Override public String toString() {
            return "OAuth.Error: " + type + " - " + details;
        }
    }

    @Deprecated
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
