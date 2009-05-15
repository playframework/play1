package play.libs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.TraceMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
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
 *    response = WS.GET("http://search.yahoo.com/search?p=<em>%s</em>&pstart=1&b=<em>%l</em>", "Google killed me", 30 );
 *    if( response.getStatus() == 200 )
 *       html = response.getString();
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
                // get inetAddersses
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
     * Make a GET request using an url template.
     * If you provide no parameters, the url is left unchanged (no replacements)
     * @param url an URL template with placeholders for parameters
     * @param params a variable list of parameters to be replaced in the template
     * @return server response {@link HttpResponse}
     */
    public static HttpResponse GET(String url, Object... params) {
        return GET(null, url, params);
    }

    /**
     * Make a GET request using an url template.
     * If you provide no parameters, the url is left unchanged (no replacements)
     * @param headers a map of headers to be appended to the request.
     * @param url an template with <tt>%s,%d</tt> placeholders for parameters.
     * @param params parameters to be replaced in the url template
     * @return HTTP response {@link HttpResponse}
     */
    public static HttpResponse GET(Map<String, Object> headers, String url, Object... params) {
        if (params.length > 0) {
            url = String.format(url, params);
        }
        if (getMethod.get() != null) {
            getMethod.get().releaseConnection();
        }
        getMethod.set(new GetMethod(url));
        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    getMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }
            httpClient.executeMethod(getMethod.get());
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
     */
    public static HttpResponse POST(Map<String, String> headers, String url, File body) {
        return POST(headers, url, body, null);
    }

    private static HttpResponse POST(Map<String, String> headers, String url, File body, String mimeType) {
        if (postMethod.get() != null) {
            postMethod.get().releaseConnection();
        }
        postMethod.set(new PostMethod(url));

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

            httpClient.executeMethod(postMethod.get());
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
     */
    public static HttpResponse POST(Map<String, Object> headers, String url, Map<String, Object> parameters) {
        if (postMethod.get() != null) {
            postMethod.get().releaseConnection();
        }
        postMethod.set(new PostMethod(url));
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

            httpClient.executeMethod(postMethod.get());
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
     */
    public static HttpResponse POST(Map<String, Object> headers, String url, String body) {
        if (postMethod.get() != null) {
            postMethod.get().releaseConnection();
        }
        postMethod.set(new PostMethod(url));

        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    postMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }

            postMethod.get().setRequestEntity(new StringRequestEntity(body));

            httpClient.executeMethod(postMethod.get());
            return new HttpResponse(postMethod.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Make a DELETE request
     * @param url resource URL
     * @return server response {@link HttpResponse}
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
     */
    public static HttpResponse DELETE(Map<String, String> headers, String url) {
        if (deleteMethod.get() != null) {
            deleteMethod.get().releaseConnection();
        }
        deleteMethod.set(new DeleteMethod(url));
        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    deleteMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }

            httpClient.executeMethod(deleteMethod.get());
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
     */
    public static HttpResponse HEAD(Map<String, String> headers, String url, Object... params) {
        url = String.format(url, params);
        if (headMethod.get() != null) {
            headMethod.get().releaseConnection();
        }
        headMethod.set(new HeadMethod(url));

        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    headMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }
            httpClient.executeMethod(headMethod.get());
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
     */
    public static HttpResponse TRACE(Map<String, String> headers, String url, Object... params) {
        url = String.format(url, params);
        if (traceMethod.get() != null) {
            traceMethod.get().releaseConnection();
        }
        traceMethod.set(new TraceMethod(url));

        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    traceMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }
            httpClient.executeMethod(traceMethod.get());
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
     */
    public static HttpResponse OPTIONS(Map<String, String> headers, String url, Object... params) {
        url = String.format(url, params);
        if (optionsMethod.get() != null) {
            optionsMethod.get().releaseConnection();
        }
        optionsMethod.set(new OptionsMethod(url));

        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    optionsMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }
            httpClient.executeMethod(optionsMethod.get());
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
     * An HTTP response wrapper
     */
    public static class HttpResponse {

        private HttpMethodBase methodBase;

        /**
         * you shouldnt have to create an HttpResponse yourself
         * @param method
         */
        public HttpResponse(HttpMethodBase method) {
            this.methodBase = method;
        }

        /**
         * the HTTP status code
         * @return 
         */
        public Integer getStatus() {
            return this.methodBase.getStatusCode();
        }

        /**
         * Parse and get the response body as a {@link Document DOM document}
         * @return a DOM document
         */
        public Document getXml() {
            try {
                String xml = methodBase.getResponseBodyAsString();
                StringReader reader = new StringReader(xml);
                return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(reader));
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                methodBase.releaseConnection();
            }
        }

        /**
         * parse and get the response body as a {@link Document DOM document}
         * @param encoding xml charset encoding
         * @return a DOM document
         */
        public Document getXml(String encoding) {
            try {
                InputSource source = new InputSource(methodBase.getResponseBodyAsStream());
                source.setEncoding(encoding);
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
                return doc;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                methodBase.releaseConnection();
            }
        }

        /**
         * get the response body as a string
         * @return
         */
        public String getString() {
            try {
                return methodBase.getResponseBodyAsString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                methodBase.releaseConnection();
            }
        }

        /**
         * get the response as a stream
         * @return an inputstream
         */
        public InputStream getStream() {
            try {
                return new ConnectionReleaserStream(methodBase.getResponseBodyAsStream(), methodBase);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * get the reponse body as a {@link JSONObject}
         * @return the json response
         */
        public JSONObject getJSONObject() {
            try {
                String json = methodBase.getResponseBodyAsString();
                return JSONObject.fromObject(json);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                methodBase.releaseConnection();
            }
        }

        /**
         * get the reponse body as a {@link JSONArray}
         * @return the json response as an array
         */
        public JSONArray getJSONArray() {
            try {
                String json = methodBase.getResponseBodyAsString();
                return JSONArray.fromObject(json);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                methodBase.releaseConnection();
            }
        }

        class ConnectionReleaserStream extends InputStream {

            private InputStream wrapped;
            private HttpMethodBase method;

            public ConnectionReleaserStream(InputStream wrapped, HttpMethodBase method) {
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