package play.libs;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.TraceMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ControllerThreadSocketFactory;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import play.Logger;
import play.PlayPlugin;

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

    private static HttpClient httpClient;
    private static ThreadLocal<GetMethod> getMethod = new ThreadLocal<GetMethod>();
    private static ThreadLocal<PostMethod> postMethod = new ThreadLocal<PostMethod>();
    private static ThreadLocal<DeleteMethod> deleteMethod = new ThreadLocal<DeleteMethod>();
    private static ThreadLocal<OptionsMethod> optionsMethod = new ThreadLocal<OptionsMethod>();
    private static ThreadLocal<TraceMethod> traceMethod = new ThreadLocal<TraceMethod>();
    private static ThreadLocal<HeadMethod> headMethod = new ThreadLocal<HeadMethod>();
    private static ThreadLocal<HttpState> states = new ThreadLocal<HttpState>();
    private static ThreadLocal<HttpMethod> httpMethod = new ThreadLocal<HttpMethod>();

    @Override
    public void invocationFinally() {
        Logger.trace("Releasing http client connections...");
        if (getMethod.get() != null) {
            getMethod.get().releaseConnection();
        }
        if (postMethod.get() != null) {
            postMethod.get().releaseConnection();
        }
        if (deleteMethod.get() != null) {
            deleteMethod.get().releaseConnection();
        }
        if (optionsMethod.get() != null) {
            optionsMethod.get().releaseConnection();
        }
        if (traceMethod.get() != null) {
            traceMethod.get().releaseConnection();
        }
        if (headMethod.get() != null) {
            headMethod.get().releaseConnection();
        }
        if (states.get() != null) {
            states.get().clear();
        }
        if (httpMethod.get() != null) {
            httpMethod.get().releaseConnection();
        }
    }
    

    static {
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        ProtocolSocketFactory factory = new ProtocolSocketFactory() {

            /**
             * @see #createSocket(java.lang.String,int,java.net.InetAddress,int)
             */
            public Socket createSocket(
                    String host,
                    int port,
                    InetAddress localAddress,
                    int localPort) throws IOException, UnknownHostException {
                
                // get inetAddresses
                InetAddress[] inetAddresses = InetAddress.getAllByName(host);


                for (int i = 0; i < inetAddresses.length; i++) {
                    try {
                        Socket socket = new Socket(inetAddresses[i], port, localAddress,
                                localPort);

                        return socket;
                    } catch (SocketException se) {
                        System.out.println("Socket exception on " + inetAddresses[i]);
                    }
                }
                // tried all
                throw new SocketException("Cannot connect to " + host);
            }

            public Socket createSocket(
                    final String host,
                    final int port,
                    final InetAddress localAddress,
                    final int localPort,
                    final HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
                if (params == null) {
                    throw new IllegalArgumentException("Parameters may not be null");
                }
                int timeout = params.getConnectionTimeout();
                if (timeout == 0) {
                    return createSocket(host, port, localAddress, localPort);
                }
                return ControllerThreadSocketFactory.createSocket(
                        this, host, port, localAddress, localPort, timeout);
            }

            /**
             * @see ProtocolSocketFactory#createSocket(java.lang.String,int,InetAddress,int)
            )
             */
            public Socket createSocket(String host, int port) throws IOException,
                    UnknownHostException {
                return createSocket(host, port, null, 0);
            }
        };

        Protocol protocol = new Protocol("http", factory, 80);
        Protocol.registerProtocol("http", protocol);
        httpClient = new HttpClient(connectionManager);

    }

    /**
     * define client authentication for a server host 
     * provided credentials will be used during the request
     * @param username
     * @param password
     * @param url hostname url or null to authenticate on any hosts
     */
    public static void authenticate(String username, String password, String url) {
        Credentials credentials = new UsernamePasswordCredentials(username, password);
        AuthScope scope = AuthScope.ANY;
        if (url != null) {
            try {
                URL oUrl = new URL(url);
                scope = new AuthScope(oUrl.getHost(), oUrl.getPort(), AuthScope.ANY_REALM);
            } catch (MalformedURLException muex) {
                throw new RuntimeException(muex);
            }
        }
        authenticate(credentials, scope);
    }

    public static void authenticate(String username, String password) {
        authenticate(username, password, null);
    }

    public static void authenticate(Credentials credentials, AuthScope authScope) {
        if (states.get() == null) {
            states.set(new HttpState());
        }
        states.get().setCredentials(authScope, credentials);
    }

    /**
     * Make a GET request using an url template.
     * If you provide no parameters, the url is left unchanged (no replacements)
     * @param url an URL template with placeholders for parameters
     * @param params a variable list of parameters to be replaced in the template using the String.format(...) syntax
     * @return server response {@link HttpResponse}
     * @deprecated Use WS.url(url, params...).get() instead.
     */
    public static HttpResponse GET(String url, Object... params) {
        return GET(null, url, params);
    }

    /**
     * Make a GET request using an url template.
     * If you provide no parameters, the url is left unchanged (no replacements)
     * @param url an URL template with placeholders for parameters
     * @param params a variable Map
     * @return server response {@link HttpResponse}
     * @deprecated Use WS.url(url).params(params).get() instead.
     */
    public static HttpResponse GET(String url, Map<String, String> params) {
        StringBuffer sb = new StringBuffer(10);
        for (String key : params.keySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(key + "=" + encode(params.get(key)));
        }
        return GET(sb.toString());
    }

    /**
     * Make a GET request using an url template.
     * If you provide no parameters, the url is left unchanged (no replacements)
     * @param headers a map of headers to be appended to the request.
     * @param url an template with <tt>%s,%d</tt> placeholders for parameters.
     * @param params parameters to be replaced in the url template using the String.format(...) syntax
     * @return HTTP response {@link HttpResponse}
     * @deprecated Use WS.url(url).params().header().get() instead.
     */
    public static HttpResponse GET(Map<String, Object> headers, String url, Object... params) {
        if (params.length > 0) {
            url = String.format(url, params);
        }
        if (getMethod.get() != null) {
            getMethod.get().releaseConnection();
        }
        getMethod.set(new GetMethod(url));
        getMethod.get().setDoAuthentication(true);
        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    getMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }
            httpClient.executeMethod(null, getMethod.get(), states.get());
            return new HttpResponse(getMethod.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Make a POST request.
     * @param url resource URL
     * @param body content to be posted. content-type is guessed from the filename extension.
     * @return server response {@link HttpResponse}
     * @deprecated Use WS.url(url).file().post() instead.
     */
    public static HttpResponse POST(String url, File body) {
        String mimeType = MimeTypes.getMimeType(body.getName());
        return POST(null, url, body, mimeType);
    }

    /**
     * Make a POST request
     * @param url resource URL
     * @param body content to be posted
     * @param mimeType the content type as a mimetype
     * @return server response {@link HttpResponse}
     * @deprecated Use WS.url(url).file().mimetype().post() instead.
     */
    public static HttpResponse POST(String url, File body, String mimeType) {
        return POST(null, url, body, mimeType);
    }

    /**
     * Make a POST request
     * @param headers a map of headers to be appended to the request.
     * @param url resource URL
     * @param body content to be posted. content-type is guessed from the filename extension.
     * @return server response {@link HttpResponse}
     * @deprecated Use WS.url(url).header().file().post() instead.
     */
    public static HttpResponse POST(Map<String, String> headers, String url, File body) {
        return POST(headers, url, body, null);
    }

    private static HttpResponse POST(Map<String, String> headers, String url, File body, String mimeType) {
        if (postMethod.get() != null) {
            postMethod.get().releaseConnection();
        }
        postMethod.set(new PostMethod(url));
        postMethod.get().setDoAuthentication(true);
        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    postMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }
            if (mimeType != null) {
                postMethod.get().addRequestHeader("content-type", mimeType);
            }

            Part[] parts = {
                new FilePart(body.getName(), body)
            };

            postMethod.get().setRequestEntity(new MultipartRequestEntity(parts, postMethod.get().getParams()));

            httpClient.executeMethod(null, postMethod.get(), states.get());
            return new HttpResponse(postMethod.get());
        } catch (Exception e) {
            postMethod.get().releaseConnection();
            throw new RuntimeException(e);
        }
    }

    /**
     * Make a POST request<p/>
     * Parameters are encoded using a <tt>application/x-www-form-urlencoded</tt> scheme.
     * You cannot send <tt>multipart/form-data</tt> for now.
     * @param url the request URL
     * @param parameters the parameters to be posted
     * @return server response {@link HttpResponse}
     * @deprecated Use WS.url(url).params().post() instead.
     */
    public static HttpResponse POST(String url, Map<String, Object> parameters) {
        return POST(null, url, parameters);
    }

    /**
     * Make a POST request<p/>
     * Parameters are encoded using a <tt>application/x-www-form-urlencoded</tt> scheme.
     * You cannot send <tt>multipart/form-data</tt> for now.
     * @param headers the request HTTP headers
     * @param url the request URL
     * @param parameters the parameters to be posted
     * @return server response {@link HttpResponse}
     * @deprecated Use WS.url(url).params().header().post() instead.
     */
    public static HttpResponse POST(Map<String, Object> headers, String url, Map<String, Object> parameters) {
        if (postMethod.get() != null) {
            postMethod.get().releaseConnection();
        }
        postMethod.set(new PostMethod(url));
        postMethod.get().setDoAuthentication(true);
        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    postMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }

            postMethod.get().addRequestHeader("content-type", "application/x-www-form-urlencoded");

            ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
            Set<String> keySet = parameters.keySet();
            for (String key : keySet) {
                Object value = parameters.get(key);
                if (value instanceof Collection) {
                    for (Object v : (Collection) value) {
                        NameValuePair nvp = new NameValuePair();
                        nvp.setName(key);
                        nvp.setValue(v.toString());
                    }
                } else {
                    NameValuePair nvp = new NameValuePair();
                    nvp.setName(key);
                    nvp.setValue(value.toString());
                }
            }

            postMethod.get().setRequestBody((NameValuePair[]) nvps.toArray());
            httpClient.executeMethod(null, postMethod.get(), states.get());
            return new HttpResponse(postMethod.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * POST some text data
     * @param url resource URL
     * @param body text content to be posted "as is", with not encoding
     * @param mimeType the request content-type
     * @return server response {@link HttpResponse}
     * @deprecated Use WS.url(url).params().body().mimetype().post() instead.
     */
    public static HttpResponse POST(String url, String body, String mimeType) {
        return POST((Map<String, Object>) null, url, body);
    }

    /**
     * POST some text data
     * @param headers extra request headers
     * @param url resource URL
     * @param body text content to be posted "as is"
     * @return server response {@link HttpResponse}
     * @deprecated Use WS.url().headers().body().post() instead
     */
    public static HttpResponse POST(Map<String, Object> headers, String url, String body) {
        if (postMethod.get() != null) {
            postMethod.get().releaseConnection();
        }
        postMethod.set(new PostMethod(url));
        postMethod.get().setDoAuthentication(true);
        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    postMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }

            postMethod.get().setRequestEntity(new StringRequestEntity(body));

            httpClient.executeMethod(null, postMethod.get(), states.get());
            return new HttpResponse(postMethod.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Make a DELETE request
     * @param url resource URL
     * @return server response {@link HttpResponse}
     * @deprecated Use WS.url().delete() instead.
     */
    public static HttpResponse DELETE(String url) {
        return DELETE(null, url);
    }

    /**
     * Make a DELETE request
     * @param headers extra request headers
     * @param url resource URL
     * @param params query parameters
     * @return server response {@link HttpResponse}
     * @deprecated Use WS.url().headers().delete() instead.
     */
    public static HttpResponse DELETE(Map<String, String> headers, String url) {
        if (deleteMethod.get() != null) {
            deleteMethod.get().releaseConnection();
        }
        deleteMethod.set(new DeleteMethod(url));
        deleteMethod.get().setDoAuthentication(true);
        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    deleteMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }

            httpClient.executeMethod(null, deleteMethod.get(), states.get());
            return new HttpResponse(deleteMethod.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Make a HEAD request
     * @param url resource URL
     * @param params query parameters
     * @return server response {@link HttpResponse}
     * @deprecated Use WS.url().head() instead.
     */
    public static HttpResponse HEAD(String url, Object... params) {
        return HEAD(null, url, params);
    }

    /**
     * Make a HEAD request
     * @param headers extra request headers
     * @param url resource URL
     * @param params query parameters
     * @return server response {@link HttpResponse}
     * @deprecated Use WS.url().headers().params().head() instead.
     */
    public static HttpResponse HEAD(Map<String, String> headers, String url, Object... params) {
        url = String.format(url, params);
        if (headMethod.get() != null) {
            headMethod.get().releaseConnection();
        }
        headMethod.set(new HeadMethod(url));
        headMethod.get().setDoAuthentication(true);
        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    headMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }
            httpClient.executeMethod(null, headMethod.get(), states.get());
            return new HttpResponse(headMethod.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Make a TRACE request
     * @param url resource URL
     * @param params query parameters
     * @return server response {@link HttpResponse}
     * @deprecated Use WS.url().trace() instead.
     */
    public static HttpResponse TRACE(String url, Object... params) {
        return TRACE(null, url, params);
    }

    /**
     * Make a TRACE request
     * @param headers extra request headers
     * @param url resource URL
     * @param params query parameters
     * @return the response
     * @deprecated Use WS.url().headers().params().delete() instead.
     */
    public static HttpResponse TRACE(Map<String, String> headers, String url, Object... params) {
        url = String.format(url, params);
        if (traceMethod.get() != null) {
            traceMethod.get().releaseConnection();
        }
        traceMethod.set(new TraceMethod(url));
        traceMethod.get().setDoAuthentication(true);
        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    traceMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }
            httpClient.executeMethod(null, traceMethod.get(), states.get());
            return new HttpResponse(traceMethod.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Make a OPTIONS request
     * @param url resource URL
     * @param params query parameters
     * @return the OPTIONS response
     * @deprecated Use WS.url().options() instead.
     */
    public static HttpResponse OPTIONS(String url, Object... params) {
        return OPTIONS(null, url, params);
    }

    /**
     * Make a OPTIONS request
     * @param headers extra request headers
     * @param url resource URL
     * @param params query parameters
     * @return the OPTIONS response
     * @deprecated Use WS.url().headers().params().options() instead.
     */
    public static HttpResponse OPTIONS(Map<String, String> headers, String url, Object... params) {
        url = String.format(url, params);
        if (optionsMethod.get() != null) {
            optionsMethod.get().releaseConnection();
        }
        optionsMethod.set(new OptionsMethod(url));
        optionsMethod.get().setDoAuthentication(true);
        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    optionsMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }
            httpClient.executeMethod(null, optionsMethod.get(), states.get());
            return new HttpResponse(optionsMethod.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        public String body;
        public FileParam[] fileParams;
        public Map<String, String> headers = new HashMap<String, String> ();
        public Map<String, Object> parameters = new HashMap<String, Object> ();
        public String mimeType;

        private WSRequest(String url) {
            this.url = url;
        }

        private void checkRelease() {
            if (httpMethod.get() != null) {
                httpMethod.get().releaseConnection();
            }
        }

        private void checkFileBody(EntityEnclosingMethod method) throws FileNotFoundException {
            EntityEnclosingMethod putOrPost = (EntityEnclosingMethod) method;
            if (this.fileParams != null) {
                //could be optimized, we know the size of this array.
                List<Part> parts = new ArrayList<Part>();
                for (int i = 0; i < this.fileParams.length; i++) {
                    parts.add(new FilePart(this.fileParams[i].paramName, this.fileParams[i].file, MimeTypes.getMimeType(this.fileParams[i].file.getName()), null));
                }
                if (this.parameters != null) {
                    for (String key : this.parameters.keySet()) {
                        Object value = this.parameters.get(key);
                        if (value instanceof Collection || value.getClass().isArray()) {
                            Collection values = value.getClass().isArray() ? Arrays.asList((Object[]) value) : (Collection) value;
                            for (Object v : (Collection) values) {
                                parts.add(new StringPart(key, v.toString(), "utf-8"));
                            }
                        } else {
                            parts.add(new StringPart(key, value.toString(), "utf-8"));
                        }
                    }
                }
                if (this.body != null) {
                    throw new RuntimeException("POST or PUT method with parameters AND body are not supported.");
                }
                putOrPost.setRequestEntity(new MultipartRequestEntity(parts.toArray(new Part[parts.size()]), putOrPost.getParams()));
                return;

            }
            if (this.parameters != null) {
                method.addRequestHeader("content-type", "application/x-www-form-urlencoded");
                putOrPost.setRequestEntity(new StringRequestEntity(createQueryString()));
            }
            if (this.body != null) {
                if (this.parameters != null && this.parameters.keySet().size()>0) {
                    throw new RuntimeException("POST or PUT method with parameters AND body are not supported.");
                }
                putOrPost.setRequestEntity(new StringRequestEntity(this.body));
            }

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
        public WSRequest body(String body) {
            this.body = body;
            return this;
        }
        
        /**
         * Add a header to the request
         * @param name header name
         * @param value header value
         * @return
         */
        public WSRequest setHeader (String name, String value) {
            this.headers.put(name,value);
            return this;
        }
        
        /**
         * Add a parameter to the request
         * @param name parameter name
         * @param value parameter value
         * @return
         */
        public WSRequest setParameter (String name, String value) {
            this.parameters.put(name,value);
            return this;
        }
        
        /**
         * Use the provided headers when executing request.
         * @param headers
         * @return the WSRequest for chaining.
         */
        public WSRequest headers(Map<String, String> headers){
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

        /** Execute a GET request.*/
        public HttpResponse get() {
            return this.executeRequest(new GetMethod(this.url));
        }

        /** Execute a POST request.*/
        public HttpResponse post() {
            return this.executeRequest(new PostMethod(this.url));
        }

        /** Execute a PUT request.*/
        public HttpResponse put() {
            return this.executeRequest(new PutMethod(this.url));
        }

        /** Execute a DELETE request.*/
        public HttpResponse delete() {
            return this.executeRequest(new DeleteMethod(this.url));
        }

        /** Execute a OPTIONS request.*/
        public HttpResponse options() {
            return this.executeRequest(new OptionsMethod(this.url));
        }

        /** Execute a HEAD request.*/
        public HttpResponse head() {
            return this.executeRequest(new HeadMethod(this.url));
        }

        /** Execute a TRACE request.*/
        public HttpResponse trace() {
            return this.executeRequest(new TraceMethod(this.url));
        }

        private HttpResponse executeRequest(HttpMethod method) {
            this.checkRelease();
            httpMethod.set(method);
            httpMethod.get().getParams().setSoTimeout(5000);
            httpMethod.get().setDoAuthentication(true);
            try {
                if (this.headers != null) {
                    for (String key : headers.keySet()) {
                        httpMethod.get().addRequestHeader(key, headers.get(key) + "");
                    }
                }
                if (httpMethod.get() instanceof EntityEnclosingMethod) {
                    this.checkFileBody((EntityEnclosingMethod) httpMethod.get());
                } else if (this.fileParams != null) {
                    throw new RuntimeException("Method " + httpMethod.get().getName() + " with file is not an option.");
                } else if (this.parameters != null && !this.parameters.isEmpty()) {
                    httpMethod.get().setQueryString(createQueryString());
                }
                if (mimeType != null) {
                    httpMethod.get().setRequestHeader("Content-Type", mimeType);
                }

                httpClient.executeMethod(null, httpMethod.get(), states.get());
                return new HttpResponse(httpMethod.get());
            } catch (Exception e) {
                httpMethod.get().releaseConnection();
                throw new RuntimeException(e);
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
                    if (value instanceof Collection || value.getClass().isArray()) {
                        Collection values = value.getClass().isArray() ? Arrays.asList((Object[]) value) : (Collection) value;
                        boolean first = true;
                        for (Object v : (Collection) values) {
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

        private HttpMethod method;

        /**
         * you shouldnt have to create an HttpResponse yourself
         * @param method
         */
        public HttpResponse(HttpMethod method) {
            this.method = method;
        }

        /**
         * the HTTP status code
         * @return 
         */
        public Integer getStatus() {
            return this.method.getStatusCode();
        }

        /**
         * The http response content type
         */
        public String getContentType() {
            return method.getResponseHeader("content-type").getValue();
        }
        
        public String getHeader(String key) {
            return method.getResponseHeader(key).getValue();
        }

        /**
         * Parse and get the response body as a {@link Document DOM document}
         * @return a DOM document
         */
        public Document getXml() {
            try {
                String xml = method.getResponseBodyAsString();
                StringReader reader = new StringReader(xml);
                return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(reader));
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                method.releaseConnection();
            }
        }

        /**
         * parse and get the response body as a {@link Document DOM document}
         * @param encoding xml charset encoding
         * @return a DOM document
         */
        public Document getXml(String encoding) {
            try {
                InputSource source = new InputSource(method.getResponseBodyAsStream());
                source.setEncoding(encoding);
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
                return doc;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                method.releaseConnection();
            }
        }

        /**
         * get the response body as a string
         * @return
         */
        public String getString() {
            try {
                return method.getResponseBodyAsString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                method.releaseConnection();
            }
        }

        /**
         * get the response as a stream
         * @return an inputstream
         */
        public InputStream getStream() {
            try {
                return new ConnectionReleaserStream(method.getResponseBodyAsStream(), method);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * get the reponse body as a {@link JSONObject}
         * @return the json response
         */
        public JsonElement getJson() {
            try {
                String json = method.getResponseBodyAsString();
                return new JsonParser().parse(json);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                method.releaseConnection();
            }
        }

        class ConnectionReleaserStream extends InputStream {

            private InputStream wrapped;
            private HttpMethod method;

            public ConnectionReleaserStream(InputStream wrapped, HttpMethod method) {
                this.wrapped = wrapped;
                this.method = method;
            }

            @Override
            public int read() throws IOException {
                return this.wrapped.read();
            }

            @Override
            public int read(byte[] arg0) throws IOException {
                return this.wrapped.read(arg0);
            }

            @Override
            public synchronized void mark(int arg0) {
                this.wrapped.mark(arg0);
            }

            @Override
            public int read(byte[] arg0, int arg1, int arg2) throws IOException {
                return this.wrapped.read(arg0, arg1, arg2);
            }

            @Override
            public synchronized void reset() throws IOException {
                this.wrapped.reset();
            }

            @Override
            public long skip(long arg0) throws IOException {
                return this.wrapped.skip(arg0);
            }

            @Override
            public int available() throws IOException {
                return this.wrapped.available();
            }

            @Override
            public boolean markSupported() {
                return this.wrapped.markSupported();
            }

            @Override
            public void close() throws IOException {
                try {
                    this.wrapped.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    if (method != null) {
                        method.releaseConnection();
                    }
                }

            }
        }
    }
}
