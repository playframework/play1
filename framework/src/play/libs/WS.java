package play.libs;

import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.mvc.Http.Header;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.FilePart;
import com.ning.http.client.Headers;
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
public class WS extends PlayPlugin {

    private static AsyncHttpClient httpClient;

    @Override
    public void onApplicationStop() {
        Logger.trace("Releasing http client connections...");
        httpClient.close();
    }

    @Override
    public void onApplicationStart() {
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

    /**
     * URL-encode an UTF-8 string to be used as a query string parameter.
     * @param part string to encode
     * @return url-encoded string
     */
    public static String encode(String part) {
        try {
            return URLEncoder.encode(part, "utf-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Build a WebService Request with the given URL.
     * This object support chaining style programming for adding params, file, headers to requests.
     * @param url of the request
     * @return a WSRequest on which you can add params, file headers using a chaining style programming.
     */
    public static WSRequest url(String url) {
        return new WSRequest(url);
    }

    /**
     * Build a WebService Request with the given URL.
     * This constructor will format url using params passed in arguments.
     * This object support chaining style programming for adding params, file, headers to requests.
     * @param url to format using the given params.
     * @param params the params passed to format the URL.
     * @return a WSRequest on which you can add params, file headers using a chaining style programming.
     */
    public static WSRequest url(String url, String... params) {
        Object[] encodedParams = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            encodedParams[i] = encode(params[i]);
        }
        return new WSRequest(String.format(url, encodedParams));
    }

    public static class WSRequest {

        public String url;
        public String username;
        public String password;
        public String body;
        public FileParam[] fileParams;
        public Map<String, String> headers = new HashMap<String, String>();
        public Map<String, Object> parameters = new HashMap<String, Object>();
        public String mimeType;
        public Integer timeout;

        private WSRequest(String url) {
            this.url = url;
        }

        /**
         * Add a MimeType to the web service request.
         * @param mimeType
         * @return the WSRequest for chaining.
         */
        public WSRequest mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        /**
         * define client authentication for a server host 
         * provided credentials will be used during the request
         * @param username
         * @param password
         */
        public WSRequest authenticate(String username, String password) {
            this.username = username;
            this.password = password;
            return this;
        }

        /**
         * Add files to request. This will only work with POST or PUT.
         * @param files
         * @return the WSRequest for chaining.
         */
        public WSRequest files(File... files) {
            this.fileParams = FileParam.getFileParams(files);
            return this;
        }

        /**
         * Add fileParams aka File and Name parameter to the request. This will only work with POST or PUT.
         * @param fileParams
         * @return the WSRequest for chaining.
         */
        public WSRequest files(FileParam... fileParams) {
            this.fileParams = fileParams;
            return this;
        }

        /**
         * Add the given body to the request.
         * @param body
         * @return the WSRequest for chaining.
         */
        public WSRequest body(Object body) {
            this.body = body == null ? "" : body.toString();
            return this;
        }

        /**
         * Add a header to the request
         * @param name header name
         * @param value header value
         * @return the WSRequest for chaining.
         */
        public WSRequest setHeader(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        /**
         * Add a parameter to the request
         * @param name parameter name
         * @param value parameter value
         * @return the WSRequest for chaining.
         */
        public WSRequest setParameter(String name, String value) {
            this.parameters.put(name, value);
            return this;
        }

        public WSRequest setParameter(String name, Object value) {
            this.parameters.put(name, value);
            return this;
        }

        /**
         * Use the provided headers when executing request.
         * @param headers
         * @return the WSRequest for chaining.
         */
        public WSRequest headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Add parameters to request.
         * If POST or PUT, parameters are passed in body using x-www-form-urlencoded if alone, or form-data if there is files too.
         * For any other method, those params are appended to the queryString. 
         * @return the WSRequest for chaining.
         */
        public WSRequest params(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Add parameters to request.
         * If POST or PUT, parameters are passed in body using x-www-form-urlencoded if alone, or form-data if there is files too.
         * For any other method, those params are appended to the queryString. 
         * @return the WSRequest for chaining.
         */
        public WSRequest setParameters(Map<String, String> parameters) {
            this.parameters.putAll(parameters);
            return this;
        }

        /** Execute a GET request synchronously. */
        public HttpResponse get() {
            try {
                return new HttpResponse(prepare(httpClient.prepareGet(url)).execute().get());
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
                return new HttpResponse(prepare(httpClient.preparePost(url)).execute().get());
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
                return new HttpResponse(prepare(httpClient.preparePut(url)).execute().get());
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
                return new HttpResponse(prepare(httpClient.prepareDelete(url)).execute().get());
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
                return new HttpResponse(prepare(httpClient.prepareOptions(url)).execute().get());
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
                return new HttpResponse(prepare(httpClient.prepareHead(url)).execute().get());
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
                return new HttpResponse(prepare(httpClient.prepareTrace(url)).execute().get());
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
                        return new HttpResponse(response);
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

        private String createQueryString() {
            StringBuilder sb = new StringBuilder();
            for (String key : this.parameters.keySet()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                Object value = this.parameters.get(key);

                if (value != null) {
                    if (value instanceof Collection<?> || value.getClass().isArray()) {
                        Collection<?> values = value.getClass().isArray() ? Arrays.asList((Object[]) value) : (Collection<?>) value;
                        boolean first = true;
                        for (Object v : values) {
                            if (!first) {
                                sb.append("&");
                            }
                            first = false;
                            sb.append(encode(key)).append("=").append(encode(v.toString()));
                        }
                    } else {
                        sb.append(encode(key)).append("=").append(encode(this.parameters.get(key).toString()));
                    }
                }
            }
            return sb.toString();
        }

    }

    public static class FileParam {
        File file;
        String paramName;

        public FileParam(File file, String name) {
            this.file = file;
            this.paramName = name;
        }

        public static FileParam[] getFileParams(File[] files) {
            FileParam[] filesp = new FileParam[files.length];
            for (int i = 0; i < files.length; i++) {
                filesp[i] = new FileParam(files[i], files[i].getName());
            }
            return filesp;
        }
    }

    /**
     * An HTTP response wrapper
     */
    public static class HttpResponse {

        private Response response;

        /**
         * you shouldnt have to create an HttpResponse yourself
         * @param method
         */
        public HttpResponse(Response response) {
            this.response = response;
        }

        /**
         * the HTTP status code
         * @return the status code of the http response
         */
        public Integer getStatus() {
            return this.response.getStatusCode();
        }

        /**
         * The http response content type
         * @return the content type of the http response
         */
        public String getContentType() {
            return getHeader("content-type");
        }

        public String getHeader(String key) {
            return response.getHeader(key);
        }

        public List<Header> getHeaders() {
            Headers hdrs = response.getHeaders();
            List<Header> result = new ArrayList<Header>();
            Iterator<Entry<String, List<String>>> iter = hdrs.iterator();
            while (iter.hasNext()) {
                Entry<String, List<String>> header = iter.next();
                result.add(new Header(header.getKey(), header.getValue()));
            }
            return result;
        }

        /**
         * Parse and get the response body as a {@link Document DOM document}
         * @return a DOM document
         */
        public Document getXml() {
            return getXml("UTF-8");
        }

        /**
         * parse and get the response body as a {@link Document DOM document}
         * @param encoding xml charset encoding
         * @return a DOM document
         */
        public Document getXml(String encoding) {
            try {
                InputSource source = new InputSource(response.getResponseBodyAsStream());
                source.setEncoding(encoding);
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
                return doc;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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

        /**
         * get the response body as a {@link com.google.gson.JSONObject}
         * @return the json response
         */
        public JsonElement getJson() {
            String json = "";
            try {
                json = response.getResponseBody();
                return new JsonParser().parse(json);
            } catch (Exception e) {
                Logger.error("Bad JSON: \n%s", json);
                throw new RuntimeException("Cannot parse JSON (check logs)", e);
            }
        }
    }
}
