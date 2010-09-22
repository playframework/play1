package play.libs.ws;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import play.Logger;
import play.Play;
import play.libs.Codec;
import play.libs.MimeTypes;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSImpl;
import play.libs.WS.WSRequest;
import play.mvc.Http.Header;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.FilePart;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Response;
import com.ning.http.client.StringPart;

/**
 * Simple HTTP client to make webservices requests.
 * 
 * <p/>
 * Get latest BBC World news as a RSS content
 * <pre>
 *    response = WS.GET("http://newsrss.bbc.co.uk/rss/newsonline_world_edition/front_page/rss.xml");
 *    Document xmldoc = response.getXml();
 *    // the real pain begins here...
 * </pre>
 * <p/>
 * 
 * Search what Yahoo! thinks of google (starting from the 30th result).
 * <pre>
 *    response = WS.GET("http://search.yahoo.com/search?p=<em>%s</em>&pstart=1&b=<em>%d</em>", "Google killed me", 30 );
 *    if( response.getStatus() == 200 ) {
 *       html = response.getString();
 *    }
 * </pre>
 */
public class WSAsync implements WSImpl {

    private static AsyncHttpClient httpClient;

    private static WSAsync uniqueInstance;

    private WSAsync() {}

    public static WSAsync getInstance() {
        if (uniqueInstance == null) {
            uniqueInstance = new WSAsync();
        }
        return uniqueInstance;
    }

    @Override
    public void init() {
        String proxyHost = Play.configuration.getProperty("http.proxyHost", System.getProperty("http.proxyHost"));
        String proxyPort = Play.configuration.getProperty("http.proxyPort", System.getProperty("http.proxyPort"));
        String proxyUser = Play.configuration.getProperty("http.proxyUser", System.getProperty("http.proxyUser"));
        String proxyPassword = Play.configuration.getProperty("http.proxyPassword", System.getProperty("http.proxyPassword"));

        Builder confBuilder = new AsyncHttpClientConfig.Builder();
        if (proxyHost != null) {
            int proxyPortInt = 0;
            try {
                proxyPortInt = Integer.parseInt(proxyPort);
            } catch (NumberFormatException e) {
                Logger.error("Cannot parse the proxy port property '%s'. Check property http.proxyPort either in System configuration or in Play config file.", proxyPort);
                throw new IllegalStateException("WS proxy is misconfigured -- check the logs for details");
            }
            ProxyServer proxy = new ProxyServer(proxyHost, proxyPortInt, proxyUser, proxyPassword);
            confBuilder.setProxyServer(proxy);
        }
        confBuilder.setFollowRedirects(true);
        httpClient = new AsyncHttpClient(confBuilder.build());
    }

    @Override
    public void stop() {
        Logger.trace("Releasing http client connections...");
        httpClient.close();
    }

    @Override
    public WSRequest newRequest(String url) {
        return new WSAsyncRequest(url);
    }

    public static class WSAsyncRequest extends WSRequest {

        private WSAsyncRequest(String url) {
            this.url = url;
        }

        /** Execute a GET request synchronously. */
        public HttpResponse get() {
            try {
                return new HttpAsyncResponse(prepare(httpClient.prepareGet(url)).execute().get());
            } catch (Exception e) {
                Logger.error(e.toString());
                throw new RuntimeException(e);
            }
        }

        /** Execute a GET request asynchronously. */
        public Future<HttpResponse> getAsync() {
            return execute(httpClient.prepareGet(url));
        }

        /** Execute a POST request.*/
        public HttpResponse post() {
            try {
                return new HttpAsyncResponse(prepare(httpClient.preparePost(url)).execute().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a POST request asynchronously.*/
        public Future<HttpResponse> postAsync() {
            return execute(httpClient.preparePost(url));
        }

        /** Execute a PUT request.*/
        public HttpResponse put() {
            try {
                return new HttpAsyncResponse(prepare(httpClient.preparePut(url)).execute().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a PUT request asynchronously.*/
        public Future<HttpResponse> putAsync() {
            return execute(httpClient.preparePut(url));
        }

        /** Execute a DELETE request.*/
        public HttpResponse delete() {
            try {
                return new HttpAsyncResponse(prepare(httpClient.prepareDelete(url)).execute().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a DELETE request asynchronously.*/
        public Future<HttpResponse> deleteAsync() {
            return execute(httpClient.prepareDelete(url));
        }

        /** Execute a OPTIONS request.*/
        public HttpResponse options() {
            try {
                return new HttpAsyncResponse(prepare(httpClient.prepareOptions(url)).execute().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a OPTIONS request asynchronously.*/
        public Future<HttpResponse> optionsAsync() {
            return execute(httpClient.prepareOptions(url));
        }

        /** Execute a HEAD request.*/
        public HttpResponse head() {
            try {
                return new HttpAsyncResponse(prepare(httpClient.prepareHead(url)).execute().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a HEAD request asynchronously.*/
        public Future<HttpResponse> headAsync() {
            return execute(httpClient.prepareHead(url));
        }

        /** Execute a TRACE request.*/
        public HttpResponse trace() {
            try {
                return new HttpAsyncResponse(prepare(httpClient.prepareTrace(url)).execute().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a TRACE request asynchronously.*/
        public Future<HttpResponse> traceAsync() {
            return execute(httpClient.prepareTrace(url));
        }

        private BoundRequestBuilder prepare(BoundRequestBuilder builder) {
            checkFileBody(builder);
            if (this.username != null && this.password != null) {
                this.headers.put("Authorization", "Basic " + Codec.encodeBASE64(this.username + ":" + this.password));
            }
            for (String key: this.headers.keySet()) {
                builder.addHeader(key, headers.get(key));
            }
            return builder;
        }

        private Future<HttpResponse> execute(BoundRequestBuilder builder) {
            try {
                return prepare(builder).execute(new AsyncCompletionHandler<HttpResponse>() {
                    @Override
                    public HttpResponse onCompleted(Response response) throws Exception {
                        return new HttpAsyncResponse(response);
                    }
                    @Override
                    public void onThrowable(Throwable t) {
                        throw new RuntimeException(t);
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void checkFileBody(BoundRequestBuilder builder) {
            if (this.fileParams != null) {
                //could be optimized, we know the size of this array.
                for (int i = 0; i < this.fileParams.length; i++) {
                    builder.addBodyPart(new FilePart(this.fileParams[i].paramName,
                            this.fileParams[i].file,
                            MimeTypes.getMimeType(this.fileParams[i].file.getName()),
                            null));
                }
                if (this.parameters != null) {
                    for (String key : this.parameters.keySet()) {
                        Object value = this.parameters.get(key);
                        if (value instanceof Collection<?> || value.getClass().isArray()) {
                            Collection<?> values = value.getClass().isArray() ? Arrays.asList((Object[]) value) : (Collection<?>) value;
                            for (Object v : values) {
                                builder.addBodyPart(new StringPart(key, v.toString()));
                            }
                        } else {
                            builder.addBodyPart(new StringPart(key, value.toString()));
                        }
                    }
                }
                return;
            }
            if (this.parameters != null && !this.parameters.isEmpty()) {
                builder.setHeader("Content-Type", "application/x-www-form-urlencoded");
                builder.setBody(createQueryString());
            }
            if (this.body != null) {
                if (this.parameters != null && !this.parameters.isEmpty()) {
                    throw new RuntimeException("POST or PUT method with parameters AND body are not supported.");
                }
                builder.setBody(this.body);
                if(this.mimeType != null) {
                    builder.setHeader("Content-Type", this.mimeType);
                }
            }
        }

    }

    /**
     * An HTTP response wrapper
     */
    public static class HttpAsyncResponse extends HttpResponse {

        private Response response;

        /**
         * you shouldnt have to create an HttpResponse yourself
         * @param method
         */
        public HttpAsyncResponse(Response response) {
            this.response = response;
        }

        /**
         * the HTTP status code
         * @return the status code of the http response
         */
        public Integer getStatus() {
            return this.response.getStatusCode();
        }

        public String getHeader(String key) {
            return response.getHeader(key);
        }

        public List<Header> getHeaders() {
            Map<String, List<String>> hdrs = response.getHeaders();
            List<Header> result = new ArrayList<Header>();
            for (String key: hdrs.keySet()) {
                result.add(new Header(key, hdrs.get(key)));
            }
            return result;
        }

        /**
         * get the response body as a string
         * @return the body of the http response
         */
        public String getString() {
            try {
                return response.getResponseBody();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * get the response as a stream
         * @return an inputstream
         */
        public InputStream getStream() {
            try {
                return response.getResponseBodyAsStream();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

}
