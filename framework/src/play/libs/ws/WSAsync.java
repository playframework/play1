package play.libs.ws;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang.NotImplementedException;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.Realm.RealmBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.multipart.ByteArrayPart;
import com.ning.http.client.multipart.FilePart;
import com.ning.http.client.multipart.Part;

import oauth.signpost.AbstractOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.http.HttpRequest;
import play.Logger;
import play.Play;
import play.libs.F.Promise;
import play.libs.MimeTypes;
import play.libs.OAuth.ServiceInfo;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSImpl;
import play.libs.WS.WSRequest;
import play.mvc.Http.Header;

/**
 * Simple HTTP client to make webservices requests.
 * 
 * <p>
 * Get latest BBC World news as a RSS content
 * 
 * <pre>
 * HttpResponse response = WS.url("http://newsrss.bbc.co.uk/rss/newsonline_world_edition/front_page/rss.xml").get();
 * Document xmldoc = response.getXml();
 * // the real pain begins here...
 * </pre>
 * <p>
 * 
 * Search what Yahoo! thinks of google (starting from the 30th result).
 * 
 * <pre>
 * HttpResponse response = WS.url("http://search.yahoo.com/search?p=<em>%s</em>&amp;pstart=1&amp;b=<em>%s</em>", "Google killed me", "30").get();
 * if (response.getStatus() == 200) {
 *     html = response.getString();
 * }
 * </pre>
 */
public class WSAsync implements WSImpl {

    private AsyncHttpClient httpClient;
    private static SSLContext sslCTX = null;

    public WSAsync() {
        String proxyHost = Play.configuration.getProperty("http.proxyHost", System.getProperty("http.proxyHost"));
        String proxyPort = Play.configuration.getProperty("http.proxyPort", System.getProperty("http.proxyPort"));
        String proxyUser = Play.configuration.getProperty("http.proxyUser", System.getProperty("http.proxyUser"));
        String proxyPassword = Play.configuration.getProperty("http.proxyPassword", System.getProperty("http.proxyPassword"));
        String nonProxyHosts = Play.configuration.getProperty("http.nonProxyHosts", System.getProperty("http.nonProxyHosts"));
        String userAgent = Play.configuration.getProperty("http.userAgent");
        String keyStore = Play.configuration.getProperty("ssl.keyStore", System.getProperty("javax.net.ssl.keyStore"));
        String keyStorePass = Play.configuration.getProperty("ssl.keyStorePassword", System.getProperty("javax.net.ssl.keyStorePassword"));
        Boolean CAValidation = Boolean.parseBoolean(Play.configuration.getProperty("ssl.cavalidation", "true"));

        Builder confBuilder = new AsyncHttpClientConfig.Builder();
        if (proxyHost != null) {
            int proxyPortInt = 0;
            try {
                proxyPortInt = Integer.parseInt(proxyPort);
            } catch (NumberFormatException e) {
                Logger.error(e,
                        "Cannot parse the proxy port property '%s'. Check property http.proxyPort either in System configuration or in Play config file.",
                        proxyPort);
                throw new IllegalStateException("WS proxy is misconfigured -- check the logs for details");
            }
            ProxyServer proxy = new ProxyServer(proxyHost, proxyPortInt, proxyUser, proxyPassword);
            if (nonProxyHosts != null) {
                String[] strings = nonProxyHosts.split("\\|");
                for (String uril : strings) {
                    proxy.addNonProxyHost(uril);
                }
            }
            confBuilder.setProxyServer(proxy);
        }
        if (userAgent != null) {
            confBuilder.setUserAgent(userAgent);
        }

        if (keyStore != null && !keyStore.equals("")) {

            Logger.info("Keystore configured, loading from '%s', CA validation enabled : %s", keyStore, CAValidation);
            if (Logger.isTraceEnabled()) {
                Logger.trace("Keystore password : %s, SSLCTX : %s", keyStorePass, sslCTX);
            }

            if (sslCTX == null) {
                sslCTX = WSSSLContext.getSslContext(keyStore, keyStorePass, CAValidation);
                confBuilder.setSSLContext(sslCTX);
            }
        }
        // when using raw urls, AHC does not encode the params in url.
        // this means we can/must encode it(with correct encoding) before
        // passing it to AHC
        confBuilder.setDisableUrlEncodingForBoundedRequests(true);
        httpClient = new AsyncHttpClient(confBuilder.build());
    }

    @Override
    public void stop() {
        Logger.trace("Releasing http client connections...");
        httpClient.close();
    }

    @Override
    public WSRequest newRequest(String url, String encoding) {
        return new WSAsyncRequest(url, encoding);
    }

    public class WSAsyncRequest extends WSRequest {

        protected String type = null;
        private String generatedContentType = null;

        protected WSAsyncRequest(String url, String encoding) {
            super(url, encoding);
        }

        /**
         * Returns the URL but removed the queryString-part of it The QueryString-info is later added with
         * addQueryString()
         * 
         * @return The URL without the queryString-part
         */
        protected String getUrlWithoutQueryString() {
            int i = url.indexOf('?');
            if (i > 0) {
                return url.substring(0, i);
            } else {
                return url;
            }
        }

        /**
         * Adds the queryString-part of the url to the BoundRequestBuilder
         * 
         * @param requestBuilder
         *            : The request buider to add the queryString-part
         */
        protected void addQueryString(BoundRequestBuilder requestBuilder) {

            // AsyncHttpClient is by default encoding everything in utf-8 so for
            // us to be able to use
            // different encoding we have configured AHC to use raw urls. When
            // using raw urls,
            // AHC does not encode url and QueryParam with utf-8 - but there is
            // another problem:
            // If we send raw (none-encoded) url (with queryString) to AHC, it
            // does not url-encode it,
            // but transform all illegal chars to '?'.
            // If we pre-encoded the url with QueryString before sending it to
            // AHC, ahc will decode it, and then
            // later break it with '?'.

            // This method basically does the same as
            // RequestBuilderBase.buildUrl() except from destroying the
            // pre-encoding

            // does url contain query_string?
            int i = url.indexOf('?');
            if (i > 0) {

                try {
                    // extract query-string-part
                    String queryPart = url.substring(i + 1);

                    // parse queryPart - and decode it... (it is going to be
                    // re-encoded later)
                    for (String param : queryPart.split("&")) {

                        i = param.indexOf('=');
                        String name;
                        String value = null;
                        if (i <= 0) {
                            // only a flag
                            name = URLDecoder.decode(param, encoding);
                        } else {
                            name = URLDecoder.decode(param.substring(0, i), encoding);
                            value = URLDecoder.decode(param.substring(i + 1), encoding);
                        }

                        if (value == null) {
                            requestBuilder.addQueryParam(URLEncoder.encode(name, encoding), null);
                        } else {
                            requestBuilder.addQueryParam(URLEncoder.encode(name, encoding), URLEncoder.encode(value, encoding));
                        }

                    }
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("Error parsing query-part of url", e);
                }
            }
        }

        private BoundRequestBuilder prepareAll(BoundRequestBuilder requestBuilder) {
            checkFileBody(requestBuilder);
            addQueryString(requestBuilder);
            addGeneratedContentType(requestBuilder);
            return requestBuilder;
        }

        public BoundRequestBuilder prepareGet() {
            return prepareAll(httpClient.prepareGet(getUrlWithoutQueryString()));
        }

        public BoundRequestBuilder prepareOptions() {
            return prepareAll(httpClient.prepareOptions(getUrlWithoutQueryString()));
        }

        public BoundRequestBuilder prepareHead() {
            return prepareAll(httpClient.prepareHead(getUrlWithoutQueryString()));
        }

        public BoundRequestBuilder preparePatch() {
            return prepareAll(httpClient.preparePatch(getUrlWithoutQueryString()));
        }

        public BoundRequestBuilder preparePost() {
            return prepareAll(httpClient.preparePost(getUrlWithoutQueryString()));
        }

        public BoundRequestBuilder preparePut() {
            return prepareAll(httpClient.preparePut(getUrlWithoutQueryString()));
        }

        public BoundRequestBuilder prepareDelete() {
            return prepareAll(httpClient.prepareDelete(getUrlWithoutQueryString()));
        }

        /** Execute a GET request synchronously. */
        @Override
        public HttpResponse get() {
            this.type = "GET";
            sign();
            try {
                return new HttpAsyncResponse(prepare(prepareGet()).execute().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a GET request asynchronously. */
        @Override
        public Promise<HttpResponse> getAsync() {
            this.type = "GET";
            sign();
            return execute(prepareGet());
        }

        /** Execute a PATCH request. */
        @Override
        public HttpResponse patch() {
            this.type = "PATCH";
            sign();
            try {
                return new HttpAsyncResponse(prepare(preparePatch()).execute().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a PATCH request asynchronously. */
        @Override
        public Promise<HttpResponse> patchAsync() {
            this.type = "PATCH";
            sign();
            return execute(preparePatch());
        }

        /** Execute a POST request. */
        @Override
        public HttpResponse post() {
            this.type = "POST";
            sign();
            try {
                return new HttpAsyncResponse(prepare(preparePost()).execute().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a POST request asynchronously. */
        @Override
        public Promise<HttpResponse> postAsync() {
            this.type = "POST";
            sign();
            return execute(preparePost());
        }

        /** Execute a PUT request. */
        @Override
        public HttpResponse put() {
            this.type = "PUT";
            try {
                return new HttpAsyncResponse(prepare(preparePut()).execute().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a PUT request asynchronously. */
        @Override
        public Promise<HttpResponse> putAsync() {
            this.type = "PUT";
            return execute(preparePut());
        }

        /** Execute a DELETE request. */
        @Override
        public HttpResponse delete() {
            this.type = "DELETE";
            try {
                return new HttpAsyncResponse(prepare(prepareDelete()).execute().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a DELETE request asynchronously. */
        @Override
        public Promise<HttpResponse> deleteAsync() {
            this.type = "DELETE";
            return execute(prepareDelete());
        }

        /** Execute a OPTIONS request. */
        @Override
        public HttpResponse options() {
            this.type = "OPTIONS";
            try {
                return new HttpAsyncResponse(prepare(prepareOptions()).execute().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a OPTIONS request asynchronously. */
        @Override
        public Promise<HttpResponse> optionsAsync() {
            this.type = "OPTIONS";
            return execute(prepareOptions());
        }

        /** Execute a HEAD request. */
        @Override
        public HttpResponse head() {
            this.type = "HEAD";
            try {
                return new HttpAsyncResponse(prepare(prepareHead()).execute().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a HEAD request asynchronously. */
        @Override
        public Promise<HttpResponse> headAsync() {
            this.type = "HEAD";
            return execute(prepareHead());
        }

        /** Execute a TRACE request. */
        @Override
        public HttpResponse trace() {
            this.type = "TRACE";
            throw new NotImplementedException();
        }

        /** Execute a TRACE request asynchronously. */
        @Override
        public Promise<HttpResponse> traceAsync() {
            this.type = "TRACE";
            throw new NotImplementedException();
        }

        private WSRequest sign() {
            if (this.oauthToken != null && this.oauthSecret != null) {
                WSOAuthConsumer consumer = new WSOAuthConsumer(oauthInfo, oauthToken, oauthSecret);
                try {
                    consumer.sign(this, this.type);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return this;
        }

        private BoundRequestBuilder prepare(BoundRequestBuilder builder) {
            if (this.username != null && this.password != null && this.scheme != null) {
                AuthScheme authScheme;
                switch (this.scheme) {
                case DIGEST:
                    authScheme = AuthScheme.DIGEST;
                    break;
                case NTLM:
                    authScheme = AuthScheme.NTLM;
                    break;
                case KERBEROS:
                    authScheme = AuthScheme.KERBEROS;
                    break;
                case SPNEGO:
                    authScheme = AuthScheme.SPNEGO;
                    break;
                case BASIC:
                    authScheme = AuthScheme.BASIC;
                    break;
                default:
                    throw new RuntimeException("Scheme " + this.scheme + " not supported by the UrlFetch WS backend.");
                }
                builder.setRealm((new RealmBuilder()).setScheme(authScheme).setPrincipal(this.username).setPassword(this.password)
                        .setUsePreemptiveAuth(true).build());
            }
            for (String key : this.headers.keySet()) {
                builder.addHeader(key, headers.get(key));
            }
            builder.setFollowRedirects(this.followRedirects);
            builder.setRequestTimeout(this.timeout * 1000);
            if (this.virtualHost != null) {
                builder.setVirtualHost(this.virtualHost);
            }
            return builder;
        }

        private Promise<HttpResponse> execute(BoundRequestBuilder builder) {
            try {
                final Promise<HttpResponse> smartFuture = new Promise<>();
                prepare(builder).execute(new AsyncCompletionHandler<HttpResponse>() {
                    @Override
                    public HttpResponse onCompleted(Response response) throws Exception {
                        HttpResponse httpResponse = new HttpAsyncResponse(response);
                        smartFuture.invoke(httpResponse);
                        return httpResponse;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        // An error happened - must "forward" the exception to
                        // the one waiting for the result
                        smartFuture.invokeWithException(t);
                    }
                });

                return smartFuture;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void checkFileBody(BoundRequestBuilder builder) {
            setResolvedContentType(null);
            if (this.fileParams != null) {
                // could be optimized, we know the size of this array.
                for (int i = 0; i < this.fileParams.length; i++) {
                    builder.addBodyPart(new FilePart(this.fileParams[i].paramName, this.fileParams[i].file,
                            MimeTypes.getMimeType(this.fileParams[i].file.getName()), Charset.forName(encoding)));
                }
                if (this.parameters != null) {
                    try {
                        // AHC only supports ascii chars in keys in multipart
                        for (String key : this.parameters.keySet()) {
                            Object value = this.parameters.get(key);
                            if (value instanceof Collection<?> || value.getClass().isArray()) {
                                Collection<?> values = value.getClass().isArray() ? Arrays.asList((Object[]) value) : (Collection<?>) value;
                                for (Object v : values) {
                                    Part part = new ByteArrayPart(key, v.toString().getBytes(encoding), "text/plain",
                                            Charset.forName(encoding), null);
                                    builder.addBodyPart(part);
                                }
                            } else {
                                Part part = new ByteArrayPart(key, value.toString().getBytes(encoding), "text/plain",
                                        Charset.forName(encoding), null);
                                builder.addBodyPart(part);
                            }
                        }
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }

                // Don't have to set content-type: AHC will automatically choose
                // multipart

                return;
            }
            if (this.parameters != null && !this.parameters.isEmpty()) {
                boolean isPostPut = "POST".equals(this.type) || ("PUT".equals(this.type));

                if (isPostPut) {
                    // Since AHC is hard-coded to encode to use UTF-8, we must
                    // build
                    // the content ourself..
                    StringBuilder sb = new StringBuilder();

                    for (String key : this.parameters.keySet()) {
                        Object value = this.parameters.get(key);
                        if (value == null)
                            continue;

                        if (value instanceof Collection<?> || value.getClass().isArray()) {
                            Collection<?> values = value.getClass().isArray() ? Arrays.asList((Object[]) value) : (Collection<?>) value;
                            for (Object v : values) {
                                if (sb.length() > 0) {
                                    sb.append('&');
                                }
                                sb.append(encode(key));
                                sb.append('=');
                                sb.append(encode(v.toString()));
                            }
                        } else {
                            // Since AHC is hard-coded to encode using UTF-8, we
                            // must build
                            // the content ourself..
                            if (sb.length() > 0) {
                                sb.append('&');
                            }
                            sb.append(encode(key));
                            sb.append('=');
                            sb.append(encode(value.toString()));
                        }
                    }
                    try {
                        byte[] bodyBytes = sb.toString().getBytes(this.encoding);
                        builder.setBody(bodyBytes);
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }

                    setResolvedContentType("application/x-www-form-urlencoded; charset=" + encoding);

                } else {
                    for (String key : this.parameters.keySet()) {
                        Object value = this.parameters.get(key);
                        if (value == null)
                            continue;
                        if (value instanceof Collection<?> || value.getClass().isArray()) {
                            Collection<?> values = value.getClass().isArray() ? Arrays.asList((Object[]) value) : (Collection<?>) value;
                            for (Object v : values) {
                                // must encode it since AHC uses raw urls
                                builder.addQueryParam(encode(key), encode(v.toString()));
                            }
                        } else {
                            // must encode it since AHC uses raw urls
                            builder.addQueryParam(encode(key), encode(value.toString()));
                        }
                    }
                    setResolvedContentType("text/html; charset=" + encoding);
                }
            }
            if (this.body != null) {
                if (this.parameters != null && !this.parameters.isEmpty()) {
                    throw new RuntimeException("POST or PUT method with parameters AND body are not supported.");
                }
                if (this.body instanceof InputStream) {
                    builder.setBody((InputStream) this.body);
                } else {
                    try {
                        byte[] bodyBytes = this.body.toString().getBytes(this.encoding);
                        builder.setBody(bodyBytes);
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }
                setResolvedContentType("text/html; charset=" + encoding);
            }

            if (this.mimeType != null) {
                // User has specified mimeType
                this.headers.put("Content-Type", this.mimeType);
            }
        }

        /**
         * Sets the resolved Content-type - This is added as Content-type-header to AHC if ser has not specified
         * Content-type or mimeType manually (Cannot add it directly to this.header since this cause problem when
         * Request-object is used multiple times with first GET, then POST)
         */
        private void setResolvedContentType(String contentType) {
            generatedContentType = contentType;
        }

        /**
         * If generatedContentType is present AND if Content-type header is not already present, add
         * generatedContentType as Content-Type to headers in requestBuilder
         */
        private void addGeneratedContentType(BoundRequestBuilder requestBuilder) {
            if (!headers.containsKey("Content-Type") && generatedContentType != null) {
                requestBuilder.addHeader("Content-Type", generatedContentType);
            }
        }

    }

    /**
     * An HTTP response wrapper
     */
    public static class HttpAsyncResponse extends HttpResponse {

        private Response response;

        /**
         * You shouldn't have to create an HttpResponse yourself
         * 
         * @param response
         *            The given response
         */
        public HttpAsyncResponse(Response response) {
            this.response = response;
        }

        /**
         * The HTTP status code
         * 
         * @return the status code of the http response
         */
        @Override
        public Integer getStatus() {
            return this.response.getStatusCode();
        }

        /**
         * the HTTP status text
         * 
         * @return the status text of the http response
         */
        @Override
        public String getStatusText() {
            return this.response.getStatusText();
        }

        @Override
        public String getHeader(String key) {
            return response.getHeader(key);
        }

        @Override
        public List<Header> getHeaders() {
            Map<String, List<String>> hdrs = response.getHeaders();
            List<Header> result = new ArrayList<>();
            for (String key : hdrs.keySet()) {
                result.add(new Header(key, hdrs.get(key)));
            }
            return result;
        }

        @Override
        public String getString() {
            try {
                return response.getResponseBody(getEncoding());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String getString(String encoding) {
            try {
                return response.getResponseBody(encoding);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * get the response as a stream
         * 
         * @return an inputstream
         */
        @Override
        public InputStream getStream() {
            try {
                return response.getResponseBodyAsStream();
            } catch (IllegalStateException e) {
                return new ByteArrayInputStream(new byte[] {}); // Workaround
                                                                // AHC's bug on
                                                                // empty
                                                                // responses
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    private static class WSOAuthConsumer extends AbstractOAuthConsumer {

        public WSOAuthConsumer(String consumerKey, String consumerSecret) {
            super(consumerKey, consumerSecret);
        }

        public WSOAuthConsumer(ServiceInfo info, String token, String secret) {
            super(info.consumerKey, info.consumerSecret);
            setTokenWithSecret(token, secret);
        }

        @Override
        protected HttpRequest wrap(Object request) {
            if (!(request instanceof WSRequest)) {
                throw new IllegalArgumentException("WSOAuthConsumer expects requests of type play.libs.WS.WSRequest");
            }
            return new WSRequestAdapter((WSRequest) request);
        }

        public WSRequest sign(WSRequest request, String method)
                throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
            WSRequestAdapter req = (WSRequestAdapter) wrap(request);
            req.setMethod(method);
            sign(req);
            return request;
        }

        public class WSRequestAdapter implements HttpRequest {

            private WSRequest request;
            private String method;

            public WSRequestAdapter(WSRequest request) {
                this.request = request;
            }

            @Override
            public Map<String, String> getAllHeaders() {
                return request.headers;
            }

            @Override
            public String getContentType() {
                return request.mimeType;
            }

            @Override
            public Object unwrap() {
                return null;
            }

            @Override
            public String getHeader(String name) {
                return request.headers.get(name);
            }

            @Override
            public InputStream getMessagePayload() throws IOException {
                return null;
            }

            @Override
            public String getMethod() {
                return this.method;
            }

            private void setMethod(String method) {
                this.method = method;
            }

            @Override
            public String getRequestUrl() {
                return request.url;
            }

            @Override
            public void setHeader(String name, String value) {
                request.setHeader(name, value);
            }

            @Override
            public void setRequestUrl(String url) {
                request.url = url;
            }

        }

    }

}
