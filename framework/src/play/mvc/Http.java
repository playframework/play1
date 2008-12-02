package play.mvc;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import play.libs.Time;

/**
 * HTTP interface
 */
public class Http {

    /**
     * An HTTP Header
     */
    public static class Header {

        /**
         * Header name
         */
        public String name;
        
        /**
         * Header value
         */
        public List<String> values;

        /**
         * First value
         * @return The first value
         */
        public String value() {
            return values.get(0);
        }
    }

    /**
     * An HTTP Cookie
     */
    public static class Cookie {

        /**
         * Cookie name
         */
        public String name;
        
        /**
         * Cookie path
         */
        public String path = "/";
        
        /**
         * for HTTPS ?
         */
        public boolean secure = false;
        
        
        /**
         * Cookie value
         */
        public String value;
    }

    /**
     * An HTTP Request
     */
    public static class Request {

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
         * HTPP method
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
        public Boolean secure;
        
        /**
         * HTTP Headers
         */
        public Map<String, Http.Header> headers = new HashMap<String, Http.Header>();
        
        /**
         * HTTP Cookies
         */
        public Map<String, Http.Cookie> cookies = new HashMap<String, Http.Cookie>();
        
        /**
         * Body stream
         */
        public InputStream body;
        
        /**
         * Additinal HTTP params extracted from route
         */
        public Map<String, String> routeArgs;
        
        /**
         * Format (html,xml,json,text)
         */
        public String format = "html";       
        
        /**
         * Full action (ex: Application.index)
         */
        public String action;    
        
        /**
         * Bind to thread
         */
        public static ThreadLocal<Request> current = new ThreadLocal<Request>();

        /**
         * Automatically resolve request format from the Accept header
         * (in this order : html > xml > json > text)
         */
        public void resolveFormat () {
        	if (headers.get("accept")==null) {
        		format="html";
        		return;
        	}
        	
        	String accept = headers.get("accept").value();
        	
        	if (accept.indexOf("application/xhtml") !=-1 || accept.indexOf("text/html")!=-1) {
        		format="html";
        		return;
        	}
                
                if (accept.indexOf("application/xml")!=-1 || accept.indexOf("text/xml")!=-1) {
        		format="xml";
        		return;
        	}
        	
        	if (accept.indexOf("text/plain")!=-1) {
        		format="txt";
        		return;
        	}
        	
        	if (accept.indexOf("application/json")!=-1 || accept.indexOf("text/javascript")!=-1) {
        		format="json";
        		return;
        	}
        
        }
        
        /**
         * Retrieve the current request
         * @return
         */
        public static Request current() {
            return current.get();
        }

        /**
         * Get the request base (ex: http://localhost:9000
         * @return
         */
        public String getBase() {
            if (port == 80 || port == 443) {
                return String.format("%s://%s", secure ? "https" : "http", domain);
            }
            return String.format("%s://%s:%s", secure ? "https" : "http", domain, port);
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
        public Map<String, Http.Header> headers = new HashMap<String, Header>();
        
        /**
         * Response cookies
         */
        public Map<String, Http.Cookie> cookies = new HashMap<String, Cookie>();
        
        /**
         * Response body stream
         */
        public OutputStream out;
        
        /**
         * Send this file directly
         */
        public File direct;
        
        /**
         * Bind to thread
         */
        public static ThreadLocal<Response> current = new ThreadLocal<Response>();

        /**
         * Retrieve the current response
         * @return
         */
        public static Response current() {
            return current.get();
        }

        /**
         * Set a response Header
         * @param name Header name
         * @param value Header value
         */
        public void setHeader(String name, String value) {
            Header h = new Header();
            h.name = name.toLowerCase();
            h.values = new ArrayList<String>();
            h.values.add(value);
            headers.put(h.name, h);
        }

        /**
         * Set a new cookie
         * @param name Cookie name
         * @param value Cookie value
         */
        public void setCookie(String name, String value) {
            if (cookies.containsKey(name)) {
                cookies.get(name).value = value;
            } else {
                Cookie cookie = new Cookie();
                cookie.name = name;
                cookie.value = value;
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
                
    }
}
