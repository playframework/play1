package play.mvc;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import play.Logger;
import play.Play;
import play.data.parsing.DataParser;
import play.exceptions.UnexpectedException;
import play.libs.Crypto;
import play.libs.Utils;

public class Scope {

    public static class Flash {

        Map<String, String> data = new HashMap<String, String>();
        Map<String, String> out = new HashMap<String, String>();
        static Pattern flashParser = Pattern.compile("\u0000([^:]*):([^\u0000]*)\u0000");

        static Flash restore() {
            try {
                Flash flash = new Flash();
                Http.Cookie cookie = Http.Request.current().cookies.get("PLAY_FLASH");
                if (cookie != null) {
                    String flashData = URLDecoder.decode(cookie.value, "utf-8");
                    Matcher matcher = flashParser.matcher(flashData);
                    while (matcher.find()) {
                        flash.data.put(matcher.group(1), matcher.group(2));
                    }
                }
                return flash;
            } catch (Exception e) {
                throw new UnexpectedException("Flash corrupted", e);
            }
        }       
        
        void save() {
            try {
                StringBuilder flash = new StringBuilder();
                for (String key : out.keySet()) {
                    flash.append("\u0000");
                    flash.append(key);
                    flash.append(":");
                    flash.append(out.get(key));
                    flash.append("\u0000");
                }
                String flashData = URLEncoder.encode(flash.toString(), "utf-8");
                Http.Response.current().setCookie("PLAY_FLASH", flashData);
            } catch (Exception e) {
                throw new UnexpectedException("Flash serializationProblem", e);
            }
        }
        
        // ThreadLocal access
        static ThreadLocal<Flash> current = new ThreadLocal<Flash>();

        public static Flash current() {
            return current.get();
        }

        public void put(String key, String value) {
            if (key.contains(":")) {
                throw new IllegalArgumentException("Character ':' is invalid in a flash key.");
            }
            data.put(key, value);
            out.put(key, value);
        }
        
        public void discard(String key) {
            out.remove(key);
        }
        
        public void discard() {
            out.clear();
        }
        
        public void keep(String key) {
            if(data.containsKey(key)) {
                out.put(key, data.get(key));
            }
        }
        
        public void keep() {
            out.putAll(data);
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

    public static class Session {

        static Pattern sessionParser = Pattern.compile("\u0000([^:]*):([^\u0000]*)\u0000");

        static Session restore() {
            try {
                Session session = new Session();
                Http.Cookie cookie = Http.Request.current().cookies.get("PLAY_SESSION");
                if (cookie != null) {
                    String value = cookie.value;
                    String sign = value.substring(0, value.indexOf("-"));
                    String data = value.substring(value.indexOf("-") + 1);
                    if (sign.equals(Crypto.sign(data, Play.getSecretKey().getBytes()))) {
                        String sessionData = URLDecoder.decode(data, "utf-8");
                        Matcher matcher = sessionParser.matcher(sessionData);
                        while (matcher.find()) {
                            session.put(matcher.group(1), matcher.group(2));
                        }
                    } else {
                        Logger.warn("Corrupted session from %s", Http.Request.current().remoteAddress);
                    }
                }
                return session;
            } catch (Exception e) {
                throw new UnexpectedException("Corrupted session from "+Http.Request.current().remoteAddress, e);
            }
        }
        Map<String, String> data = new HashMap<String, String>();        // ThreadLocal access
        public static ThreadLocal<Session> current = new ThreadLocal<Session>();

        public static Session current() {
            return current.get();
        }

        void save() {
            try {
                StringBuilder session = new StringBuilder();
                for (String key : data.keySet()) {
                    session.append("\u0000");
                    session.append(key);
                    session.append(":");
                    session.append(data.get(key));
                    session.append("\u0000");
                }
                String sessionData = URLEncoder.encode(session.toString(), "utf-8");
                String sign = Crypto.sign(sessionData, Play.getSecretKey().getBytes());
                Http.Response.current().setCookie("PLAY_SESSION", sign + "-" + sessionData);
            } catch (Exception e) {
                throw new UnexpectedException("Session serializationProblem", e);
            }
        }

        public void put(String key, String value) {
            if (key.contains(":")) {
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
            if (!requestIsParsed) {
                Http.Request request = Http.Request.current();
                String contentType = request.contentType;
                if (contentType != null) {
                    DataParser dataParser = DataParser.parsers.get(contentType);
                    if (dataParser != null) {
                        _mergeWith(dataParser.parse(request.body));
                    }
                }
                requestIsParsed = true;
            }
        }

        public String get(String key) {
            checkAndParse();
            if (data.containsKey(key)) {
                return data.get(key)[0];
            }
            return null;
        }

        public String[] getAll(String key) {
            checkAndParse();
            return data.get(key);
        }
        
        public Map<String, String[]> all() {
            checkAndParse();
            return data;
        }
        
        public Map<String, String> allSimple() {
            checkAndParse();
            Map<String, String> result = new HashMap<String, String>();
            for(String key : data.keySet()) {
                result.put(key, data.get(key)[0]);
            }                            
            return result;
        }

        void _mergeWith(Map<String, String[]> map) {
            for (String key : map.keySet()) {
                Utils.Maps.mergeValueInMap(data, key, map.get(key));
            }
        }

        void __mergeWith(Map<String, String> map) {
            for (String key : map.keySet()) {
                Utils.Maps.mergeValueInMap(data, key, map.get(key));
            }
        }
        
        public String urlEncode() {
            checkAndParse();
            StringBuffer ue = new StringBuffer();
            for(String key : data.keySet()) {
                if(key.equals("body")) {
                    continue;
                }
                String[] values = data.get(key);
                for(String value : values) {
                    try {
                        ue.append(URLEncoder.encode(key, "utf-8"));
                        ue.append("=");
                        ue.append(URLEncoder.encode(value, "utf-8"));
                        ue.append("&");
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return ue.toString();
        }
    }

    public static class RenderArgs {

        Map<String, Object> data = new HashMap<String, Object>();        // ThreadLocal access
        public static ThreadLocal<RenderArgs> current = new ThreadLocal<RenderArgs>();

        public static RenderArgs current() {
            return current.get();
        }

        public void put(String key, Object arg) {
            this.data.put(key, arg);
        }
    }
}
