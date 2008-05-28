package play.mvc;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        public String domain;
        public String path;
        public Boolean secure;
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
        public Integer port;
        public Boolean secure;
        public Map<String, Http.Header> headers = new HashMap<String, Http.Header>();
        public Map<String, Http.Cookie> cookies = new HashMap<String, Http.Cookie>();
        public InputStream body;   
        public Map<String, String> routeArgs;
        public String format = "html";
        
        // Play!
        public String action;
        
        // ThreadLocal access
        public static ThreadLocal<Request> current = new ThreadLocal<Request>();
        public static Request current() {
            return current.get();
        }
        
        
    }

    public static class Response {

        // A clean access to HTTP
        public Integer status = 200;
        public String contentType;
        public Map<String, Http.Header> headers = new HashMap<String, Header>();
        public Map<String, Http.Cookie> cookies = new HashMap<String, Cookie>();
        public OutputStream out;
        
        // ThreadLocal access
        public static ThreadLocal<Response> current = new ThreadLocal<Response>();
        public static Response current() {
            return current.get();
        }
        
        public void setHeader(String name, String value) {
            Header h = new Header();
            h.name = name;
            h.values = new ArrayList<String>();
            h.values.add(value);
            headers.put(name, h);
        }
        
    }

}
