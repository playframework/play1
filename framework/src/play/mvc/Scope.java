package play.mvc;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import play.data.parsing.DataParser;
import play.exceptions.UnexpectedException;
import play.libs.Utils;

public class Scope {
    
    public static class Flash {

        // ThreadLocal access
        private static ThreadLocal<Flash> current = new ThreadLocal<Flash>();    
        public static Flash current() {
            return current.get();
        }

    }
    
    public static class Session {
        
        static Pattern sessionParser = Pattern.compile("\u0000([^:]*):([^\u0000]*)\u0000");

        static Session restore() {
            try {
                Session session = new Session();
                Http.Cookie cookie = Http.Request.current().cookies.get("PLAY_SESSION");
                if(cookie != null) {
                    String sessionData = URLDecoder.decode(cookie.value, "utf-8");
                    Matcher matcher = sessionParser.matcher(sessionData);
                    while(matcher.find()) {
                        session.put(matcher.group(1), matcher.group(2));
                    }
                }
                return session;
            } catch(Exception e) {
                throw new UnexpectedException("Session corrupted", e);
            } 
        }
        
        Map<String, String> data = new HashMap<String, String>();

        // ThreadLocal access
        public static ThreadLocal<Session> current = new ThreadLocal<Session>();    
        public static Session current() {
            return current.get();
        }

        void save() {
            try {
                StringBuilder session = new StringBuilder();
                for(String key : data.keySet()) {
                    session.append("\u0000");
                    session.append(key);
                    session.append(":");
                    session.append(data.get(key));
                    session.append("\u0000");
                }
                String sessionData = URLEncoder.encode(session.toString(), "utf-8");
                Http.Response.current().setSessionCookie("PLAY_SESSION", sessionData);
            } catch(Exception e) {
                throw new UnexpectedException("Session serializationProblem", e);
            } 
        }
        
        public void put(String key, String value) {
            if(key.contains(":")) {
                throw new IllegalArgumentException("Character ':' is invalid in a session key.");
            }
            data.put(key, value);
        }
        
        public String get(String key) {
            return data.get(key);
        }
        
        public boolean remove(String key) {
            return data.remove(key) != null;
        }
        
        public void clear() {
            data.clear();
        } 
        
        public boolean contains(String key) {
            return data.containsKey(key);
        }

    }
    
    public static class Params {
        
        // ThreadLocal access
        public static ThreadLocal<Params> current = new ThreadLocal<Params>();    
        public static Params current() {
            return current.get();
        }
        
        boolean requestIsParsed;
        Map<String, String[]> data = new HashMap<String, String[]>();
        
        void checkAndParse() {
            if(!requestIsParsed) {
                Http.Request request = Http.Request.current();
                String contentType = request.contentType;
                if(contentType != null) {
                    DataParser dataParser = DataParser.parsers.get(contentType);
                    if(dataParser != null) {
                        _mergeWith(dataParser.parse(request.body));
                    }
                }
                requestIsParsed = true;
            }
        }

        public String get(String key) {
            checkAndParse();
            if(data.containsKey(key)) {
                return data.get(key)[0];
            }
            return null;
        }
        
        public String[] getAll(String key) {
            checkAndParse();
            return data.get(key);
        }
        
        void _mergeWith(Map<String, String[]> map) {
            for(String key : map.keySet()) {
                Utils.Maps.mergeValueInMap(data, key, map.get(key));
            }
        }
        
        void __mergeWith(Map<String, String> map) {
            for(String key : map.keySet()) {
                Utils.Maps.mergeValueInMap(data, key, map.get(key));
            }
        }
        
        
    }
    
    public static class RenderArgs {
        
        Map<String, Object> data = new HashMap<String, Object>();
        
        // ThreadLocal access
        public static ThreadLocal<RenderArgs> current = new ThreadLocal<RenderArgs>();    
        public static RenderArgs current() {
            return current.get();
        }
        
        public void put(String key, Object arg) {
            this.data.put(key, arg);
        }
                
    }


}
