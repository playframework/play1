package play.mvc;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import play.Logger;
import play.Play;
import play.exceptions.UnexpectedException;
import play.libs.Codec;
import play.libs.F;
import play.libs.F.Option;
import play.libs.F.Promise;
import play.libs.F.EventStream;
import play.libs.Time;
import play.utils.Utils;

/**
 * HTTP interface
 */
public class Http {

    public static class StatusCode {

        public static final int OK = 200;
        public static final int CREATED = 201;
        public static final int ACCEPTED = 202;
        public static final int PARTIAL_INFO = 203;
        public static final int NO_RESPONSE = 204;
        public static final int MOVED = 301;
        public static final int FOUND = 302;
        public static final int METHOD = 303;
        public static final int NOT_MODIFIED = 304;
        public static final int BAD_REQUEST = 400;
        public static final int UNAUTHORIZED = 401;
        public static final int PAYMENT_REQUIRED = 402;
        public static final int FORBIDDEN = 403;
        public static final int NOT_FOUND = 404;
        public static final int INTERNAL_ERROR = 500;
        public static final int NOT_IMPLEMENTED = 501;
        public static final int OVERLOADED = 502;
        public static final int GATEWAY_TIMEOUT = 503;

        public static boolean success(int code) {
            return code / 100 == 2;
        }

        public static boolean redirect(int code) {
            return code / 100 == 3;
        }

        public static boolean error(int code) {
            return code / 100 == 4 || code / 100 == 5;
        }
    }

    /**
     * An HTTP Header
     */
    public static class Header implements Serializable {

        /**
         * Header name
         */
        public String name;
        /**
         * Header value
         */
        public List<String> values;

        public Header() {
            this.values = new ArrayList<String>(5);
        }

        public Header(String name, String value) {
            this.name = name;
            this.values = new ArrayList<String>(5);
            this.values.add(value);
        }

        public Header(String name, List<String> values) {
            this.name = name;
            this.values = values;
        }

        /**
         * First value
         * @return The first value
         */
        public String value() {
            return values.get(0);
        }

        @Override
        public String toString() {
            return values.toString();
        }
    }

    /**
     * An HTTP Cookie
     */
    public static class Cookie implements Serializable {

        /**
         * Cookie name
         */
        public String name;
        /**
         * Cookie domain
         */
        public String domain;
        /**
         * Cookie path
         */
        public String path = Play.ctxPath + "/";
        /**
         * for HTTPS ?
         */
        public boolean secure = false;
        /**
         * Cookie value
         */
        public String value;
        /**
         * Cookie max-age
         */
        public Integer maxAge;
        /**
         * Don't use
         */
        public boolean sendOnError = false;
        /**
         * See http://www.owasp.org/index.php/HttpOnly
         */
        public boolean httpOnly = false;
    }

    /**
     * An HTTP Request
     */
    public static class Request implements Serializable {

        /**
         * Server host
         */
        public String host;
        /**
         * Request path
         */
        public String path;
        /**
         * QueryString
         */
        public String querystring;
        /**
         * Full url
         */
        public String url;
        /**
         * HTTP method
         */
        public String method;
        /**
         * Server domain
         */
        public String domain;
        /**
         * Client address
         */
        public String remoteAddress;
        /**
         * Request content-type
         */
        public String contentType;
        /**
         * Controller to invoke
         */
        public String controller;
        /**
         * Action method name
         */
        public String actionMethod;
        /**
         * HTTP port
         */
        public Integer port;
        /**
         * is HTTPS ?
         */
        public Boolean secure = false;
        /**
         * HTTP Headers
         */
        public Map<String, Http.Header> headers = new HashMap<String, Http.Header>(16);
        /**
         * HTTP Cookies
         */
        public Map<String, Http.Cookie> cookies = new HashMap<String, Http.Cookie>(16);
        /**
         * Body stream
         */
        public transient InputStream body;
        /**
         * Additional HTTP params extracted from route
         */
        public Map<String, String> routeArgs;
        /**
         * Format (html,xml,json,text)
         */
        public String format = null;
        /**
         * Full action (ex: Application.index)
         */
        public String action;
        /**
         * Bind to thread
         */
        public static ThreadLocal<Request> current = new ThreadLocal<Request>();
        /**
         * The really invoker Java methid
         */
        public transient Method invokedMethod;
        /**
         * The invoked controller class
         */
        public transient Class<? extends Controller> controllerClass;
        /**
         * Free space to store your request specific data
         */
        public Map<String, Object> args = new HashMap<String, Object>(16);
        /**
         * When the request has been received
         */
        public Date date = new Date();
        /**
         * New request or already submitted
         */
        public boolean isNew = true;
        /**
         * HTTP Basic User
         */
        public String user;
        /**
         * HTTP Basic Password
         */
        public String password;
        /**
         * Request comes from loopback interface
         */
        public boolean isLoopback;
        /**
         * ActionInvoker.resolvedRoutes was called?
         */
        boolean resolved;
        /**
         * Params
         */
        public final Scope.Params params = new Scope.Params();

        public void _init() {
            Header header = headers.get("authorization");
            if (header != null && header.value().startsWith("Basic ")) {
                String data = header.value().substring(6);
                String[] decodedData = new String(Codec.decodeBASE64(data)).split(":");
                user = decodedData.length > 0 ? decodedData[0] : null;
                password = decodedData.length > 1 ? decodedData[1] : null;
            }
        }

        /**
         * Automatically resolve request format from the Accept header
         * (in this order : html > xml > json > text)
         */
        public void resolveFormat() {

            if (format != null) {
                return;
            }

            if (headers.get("accept") != null) {
            	String accept = headers.get("accept").value();
            	List<String> allowedTypes = Arrays.asList(accept.split(","));
            	List<String> sortedTypes = this.sortStrings(allowedTypes);
            	Collections.reverse(sortedTypes);
            
            	for (String type : sortedTypes) {
            		if (Play.contentTypes.containsKey(type)) {
            			format = Play.contentTypes.getProperty(type);
            			return;
            		}
            	}
            }
            
            format = Play.contentTypes.getProperty("default");
        }

        /**
         * Retrieve the current request
         * @return the current request
         */
        public static Request current() {
            return current.get();
        }

        /**
         * This request was sent by an Ajax framework.
         * (rely on the X-Requested-With header).
         */
        public boolean isAjax() {
            if (!headers.containsKey("x-requested-with")) {
                return false;
            }
            return "XMLHttpRequest".equals(headers.get("x-requested-with").value());
        }

        /**
         * Get the request base (ex: http://localhost:9000
         * @return the request base of the url (protocol, host and port)
         */
        public String getBase() {
            if (port == 80 || port == 443) {
                return String.format("%s://%s", secure ? "https" : "http", domain).intern();
            }
            return String.format("%s://%s:%s", secure ? "https" : "http", domain, port).intern();
        }

        @Override
        public String toString() {
            return method + " " + path + (querystring != null && querystring.length() > 0 ? "?" + querystring : "");
        }

        /**
         * Return the languages requested by the browser, ordered by preference (preferred first).
         * If no Accept-Language header is present, an empty list is returned.
         *
         * @return Language codes in order of preference, e.g. "en-us,en-gb,en,de".
         */
        public List<String> acceptLanguage() {
            if (!headers.containsKey("accept-language")) {
                return Collections.<String>emptyList();
            }
            String acceptLanguage = headers.get("accept-language").value();
            List<String> languages = Arrays.asList(acceptLanguage.split(","));
            
            return this.sortStrings(languages);
        }

        public boolean isModified(String etag, long last) {
            if (!(headers.containsKey("if-none-match") && headers.containsKey("if-modified-since"))) {
                return true;
            } else {
                String browserEtag = headers.get("if-none-match").value();
                if (!browserEtag.equals(etag)) {
                    return true;
                } else {
                    try {
                        Date browserDate = Utils.getHttpDateFormatter().parse(headers.get("if-modified-since").value());
                        if (browserDate.getTime() >= last) {
                            return false;
                        }
                    } catch (ParseException ex) {
                        Logger.error("Can't parse date", ex);
                    }
                    return true;
                }
            }
        }
        
        /**
         * Sorts typical HTTP header strings such as "UTF-8;q=1.0" and "MacRoman;q=0.7" according to their <em>q</em> value
         * and returns a list of strings without those values in ascending order.
         * 
         * @param strings
         * @return
         */
        private List<String> sortStrings(final List<String> strings) {
        	final Pattern qpattern = Pattern.compile("q=([0-9\\.]+)");
        	
        	Collections.sort(strings, new Comparator<String>() {
                public int compare(String first, String second) {
                    double q1 = 1.0;
                    double q2 = 1.0;
                    Matcher m1 = qpattern.matcher(first);
                    Matcher m2 = qpattern.matcher(second);
                    if (m1.find()) {
                        q1 = Double.parseDouble(m1.group(1));
                    }
                    if (m2.find()) {
                        q2 = Double.parseDouble(m2.group(1));
                    }

                    if (q1 < q2) {
                    	return -1;
                    } else if (q1 == q2) {
                    	return 0;
                    } else {
                    	return 1;
                    }
                }
            });
        	
        	List<String> result = new ArrayList<String>(10);
            for (String string : strings) {
                result.add(string.split(";")[0]);
            }
            return result;
        }
    }

    /**
     * An HTTP response
     */
    public static class Response {

        /**
         * Response status code
         */
        public Integer status = 200;
        /**
         * Response content type
         */
        public String contentType;
        /**
         * Response headers
         */
        public Map<String, Http.Header> headers = new HashMap<String, Header>(16);
        /**
         * Response cookies
         */
        public Map<String, Http.Cookie> cookies = new HashMap<String, Cookie>(16);
        /**
         * Response body stream
         */
        public ByteArrayOutputStream out;
        /**
         * Send this file directly
         */
        public Object direct;
        /**
         * Bind to thread
         */
        public static ThreadLocal<Response> current = new ThreadLocal<Response>();

        /**
         * Retrieve the current response
         * @return the current response
         */
        public static Response current() {
            return current.get();
        }

        /**
         * Get a response header
         * @param name Header name case-insensitive
         * @return the header value as a String
         */
        public String getHeader(String name) {
            for (String key : headers.keySet()) {
                if (key.toLowerCase().equals(name.toLowerCase())) {
                    if (headers.get(key) != null) {
                        return headers.get(key).value();
                    }
                }
            }
            return null;
        }

        /**
         * Set a response header
         * @param name Header name
         * @param value Header value
         */
        public void setHeader(String name, String value) {
            Header h = new Header();
            h.name = name;
            h.values = new ArrayList<String>(1);
            h.values.add(value);
            headers.put(name, h);
        }

        public void setContentTypeIfNotSet(String contentType) {
            if (this.contentType == null) {
                this.contentType = contentType;
            }
        }

        /**
         * Set a new cookie
         * @param name Cookie name
         * @param value Cookie value
         */
        public void setCookie(String name, String value) {
            setCookie(name, value, null, "/", null, false);
        }

        public void removeCookie(String name) {
            setCookie(name, "", null, "/", 0, false);
        }

        /**
         * Set a new cookie that will expire in (current) + duration
         * @param name
         * @param value
         * @param duration Ex: 3d
         */
        public void setCookie(String name, String value, String duration) {
            setCookie(name, value, null, "/", Time.parseDuration(duration), false);
        }

        public void setCookie(String name, String value, String domain, String path, Integer maxAge, boolean secure) {
            setCookie(name, value, domain, path, maxAge, secure, false);
        }

        public void setCookie(String name, String value, String domain, String path, Integer maxAge, boolean secure, boolean httpOnly) {
            path = Play.ctxPath + path;
            if (cookies.containsKey(name) && cookies.get(name).path.equals(path) && ((cookies.get(name).domain == null && domain == null) || (cookies.get(name).domain.equals(domain)))) {
                cookies.get(name).value = value;
                if (maxAge != null) {
                    cookies.get(name).maxAge = maxAge;
                }
                cookies.get(name).secure = secure;
            } else {
                Cookie cookie = new Cookie();
                cookie.name = name;
                cookie.value = value;
                cookie.path = path;
                cookie.secure = secure;
                cookie.httpOnly = httpOnly;
                if (domain != null) {
                    cookie.domain = domain;
                }
                if (maxAge != null) {
                    cookie.maxAge = maxAge;
                }
                cookies.put(name, cookie);
            }
        }

        /**
         * Add a cache-control header
         * @param duration Ex: 3h
         */
        public void cacheFor(String duration) {
            int maxAge = Time.parseDuration(duration);
            setHeader("Cache-Control", "max-age=" + maxAge);
        }

        /**
         * Add cache-control headers
         * @param duration Ex: 3h
         */
        public void cacheFor(String etag, String duration, long lastModified) {
            int maxAge = Time.parseDuration(duration);
            setHeader("Cache-Control", "max-age=" + maxAge);
            setHeader("Last-Modified", Utils.getHttpDateFormatter().format(new Date(lastModified)));
            setHeader("Etag", etag);
        }

        /**
         * Add headers to allow cross-domain requests. Be careful, a lot of browsers don't support
         * these features and will ignore the headers. Refer to the browsers' documentation to
         * know what versions support them.
         * @param allowOrigin a comma separated list of domains allowed to perform the x-domain call, or "*" for all.
         */
        public void accessControl(String allowOrigin) {
            accessControl(allowOrigin, null, false);
        }

        /**
         * Add headers to allow cross-domain requests. Be careful, a lot of browsers don't support
         * these features and will ignore the headers. Refer to the browsers' documentation to
         * know what versions support them.
         * @param allowOrigin a comma separated list of domains allowed to perform the x-domain call, or "*" for all.
         * @param allowCredentials Let the browser send the cookies when doing a x-domain request. Only respected by the browser if allowOrigin != "*"
         */
        public void accessControl(String allowOrigin, boolean allowCredentials) {
            accessControl(allowOrigin, null, allowCredentials);
        }

        /**
         * Add headers to allow cross-domain requests. Be careful, a lot of browsers don't support
         * these features and will ignore the headers. Refer to the browsers' documentation to
         * know what versions support them.
         * @param allowOrigin a comma separated list of domains allowed to perform the x-domain call, or "*" for all.
         * @param allowMethods a comma separated list of HTTP methods allowed, or null for all.
         * @param allowCredentials Let the browser send the cookies when doing a x-domain request. Only respected by the browser if allowOrigin != "*"
         */
        public void accessControl(String allowOrigin, String allowMethods, boolean allowCredentials) {
            setHeader("Access-Control-Allow-Origin", allowOrigin);
            if (allowMethods != null) {
                setHeader("Access-Control-Allow-Methods", allowMethods);
            }
            if (allowCredentials == true) {
                if (allowOrigin.equals("*")) {
                    Logger.warn("Response.accessControl: When the allowed domain is \"*\", Allow-Credentials is likely to be ignored by the browser.");
                }
                setHeader("Access-Control-Allow-Credentials", "true");
            }
        }

        public void print(Object o) {
            try {
                out.write(o.toString().getBytes("utf-8"));
            } catch (IOException ex) {
                throw new UnexpectedException("UTF-8 problem ?", ex);
            }
        }

        public void reset() {
            out.reset();
        }
        // Chunked stream
        public boolean chunked = false;
        final List<F.Action<Object>> writeChunkHandlers = new ArrayList<F.Action<Object>>();

        public void writeChunk(Object o) {
            this.chunked = true;
            if (writeChunkHandlers.isEmpty()) {
                throw new UnsupportedOperationException("Your HTTP server doesn't yet support chunked response stream");
            }
            for (F.Action<Object> handler : writeChunkHandlers) {
                handler.invoke(o);
            }
        }

        public void onWriteChunk(F.Action<Object> handler) {
            writeChunkHandlers.add(handler);
        }
    }

    /**
     * A Websocket Inbound channel
     */
    public abstract static class Inbound {

        public final static ThreadLocal<Inbound> current = new ThreadLocal<Inbound>();

        public static Inbound current() {
            return current.get();
        }
        final EventStream<WebSocketEvent> stream = new EventStream<WebSocketEvent>();

        public void _received(WebSocketFrame frame) {
            stream.publish(frame);
        }

        public Promise<WebSocketEvent> nextEvent() {
            if (!isOpen()) {
                throw new IllegalStateException("The inbound channel is closed");
            }
            return stream.nextEvent();
        }

        public void close() {
            stream.publish(new WebSocketClose());
        }

        public abstract boolean isOpen();
    }

    /**
     * A Websocket Outbound channel
     */
    public static abstract class Outbound {

        public static ThreadLocal<Outbound> current = new ThreadLocal<Outbound>();

        public static Outbound current() {
            return current.get();
        }

        public abstract void send(String data);

        public abstract void send(byte opcode, byte[] data, int offset, int length);

        public abstract boolean isOpen();

        public abstract void close();

        public void send(byte opcode, byte[] data) {
            send(opcode, data, 0, data.length);
        }

        public void send(String pattern, Object... args) {
            send(String.format(pattern, args));
        }

        public void sendJson(Object o) {
            send(new Gson().toJson(o));
        }
    }

    public static class WebSocketEvent {

        public static F.Matcher<WebSocketEvent, WebSocketClose> SocketClosed = new F.Matcher<WebSocketEvent, WebSocketClose>() {

            @Override
            public Option<WebSocketClose> match(WebSocketEvent o) {
                if (o instanceof WebSocketClose) {
                    return F.Option.Some((WebSocketClose) o);
                }
                return F.Option.None();
            }
        };
        public static F.Matcher<WebSocketEvent, String> TextFrame = new F.Matcher<WebSocketEvent, String>() {

            @Override
            public Option<String> match(WebSocketEvent o) {
                if (o instanceof WebSocketFrame) {
                    WebSocketFrame frame = (WebSocketFrame) o;
                    if (!frame.isBinary) {
                        return F.Option.Some(frame.textData);
                    }
                }
                return F.Option.None();
            }
        };
        public static F.Matcher<WebSocketEvent, byte[]> BinaryFrame = new F.Matcher<WebSocketEvent, byte[]>() {

            @Override
            public Option<byte[]> match(WebSocketEvent o) {
                if (o instanceof WebSocketFrame) {
                    WebSocketFrame frame = (WebSocketFrame) o;
                    if (frame.isBinary) {
                        return F.Option.Some(frame.binaryData);
                    }
                }
                return F.Option.None();
            }
        };
    }

    /**
     * A Websocket frame
     */
    public static class WebSocketFrame extends WebSocketEvent {

        final public boolean isBinary;
        final public String textData;
        final public byte[] binaryData;

        public WebSocketFrame(String data) {
            this.isBinary = false;
            this.textData = data;
            this.binaryData = null;
        }

        public WebSocketFrame(byte[] data) {
            this.isBinary = true;
            this.binaryData = data;
            this.textData = null;
        }
    }

    public static class WebSocketClose extends WebSocketEvent {
    }
}
