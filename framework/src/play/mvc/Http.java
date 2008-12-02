package play.mvc;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import play.libs.Time;

public class Http {

    public static class Header {

        public String name;
        public List<String> values;

        public String value() {
            return values.get(0);
        }
    }

    public static class Cookie {

        public String name;
        public String path = "/";
        public boolean secure = false;
        public String value;
    }

    public static class Request {

        // A clean access to HTTP
        public String host;
        public String path;
        public String querystring;
        public String url;
        public String method;
        public String domain;
        public String remoteAddress;
        public String remoteUser;
        public String contentType;
        public String controller;
        public String actionMethod;
        public Integer port;
        public Boolean secure;
        public Map<String, Http.Header> headers = new HashMap<String, Http.Header>();
        public Map<String, Http.Cookie> cookies = new HashMap<String, Http.Cookie>();
        public InputStream body;
        public Map<String, String> routeArgs;
        public String format = "html";        
        public String action;        
        public static ThreadLocal<Request> current = new ThreadLocal<Request>();

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
        	
        	if (accept.indexOf("text/plain")!=-1) {
        		format="txt";
        		return;
        	}
        	
        	if (accept.indexOf("application/json")!=-1 || accept.indexOf("text/javascript")!=-1) {
        		format="json";
        		return;
        	}
        
        	if (accept.indexOf("application/xml")!=-1 || accept.indexOf("text/xml")!=-1) {
        		format="xml";
        		return;
        	}
        }
        
        public static Request current() {
            return current.get();
        }

        public String getBase() {
            if (port == 80 || port == 443) {
                return String.format("%s://%s", secure ? "https" : "http", domain);
            }
            return String.format("%s://%s:%s", secure ? "https" : "http", domain, port);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() +
                    "\n  host: " + host +
                    "\n  port: " + port +
                    "\n  path: " + path +
                    "\n  secure: " + secure +
                    "\n  url: " + url +
                    "\n  querystring: " + querystring +
                    "\n  method: " + method +
                    "\n  domain: " + domain +
                    "\n  remoteAddress: " + remoteAddress +
                    "\n  remoteUser: " + remoteUser +
                    "\n  contentType: " + contentType +
                    "\n  controller: " + controller +
                    "\n  actionMethod: " + actionMethod +
                    "\n  action: " + action +
                    "\n";
        }
    }

    public static class Response {

        // A clean access to HTTP
        public Integer status = 200;
        public String contentType;
        public Map<String, Http.Header> headers = new HashMap<String, Header>();
        public Map<String, Http.Cookie> cookies = new HashMap<String, Cookie>();
        public OutputStream out;
        public File direct;
        // ThreadLocal access
        public static ThreadLocal<Response> current = new ThreadLocal<Response>();

        public static Response current() {
            return current.get();
        }

        public void setHeader(String name, String value) {
            Header h = new Header();
            h.name = name.toLowerCase();
            h.values = new ArrayList<String>();
            h.values.add(value);
            headers.put(h.name, h);
        }

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

        public void cacheFor(String duration) {
            int maxAge = Time.parseDuration(duration);
            setHeader("Cache-Control", "max-age=" + maxAge);
        }
                
    }
}
