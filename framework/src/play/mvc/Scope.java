package play.mvc;

import java.lang.annotation.Annotation;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import play.Logger;
import play.Play;
import play.data.binding.Binder;
import play.data.parsing.DataParser;
import play.data.validation.Validation;
import play.exceptions.UnexpectedException;
import play.libs.Codec;
import play.libs.Crypto;
import play.libs.Time;
import play.utils.Utils;

/**
 * All application Scopes
 */
public class Scope {

    public static final String COOKIE_PREFIX = Play.configuration.getProperty("application.session.cookie", "PLAY");
    public static final boolean COOKIE_SECURE = Play.configuration.getProperty("application.session.secure", "false").toLowerCase().equals("true");
    public static final String COOKIE_EXPIRE = Play.configuration.getProperty("application.session.maxAge");
    public static final boolean SESSION_HTTPONLY = Play.configuration.getProperty("application.session.httpOnly", "false").toLowerCase().equals("true");

    /**
     * Flash scope
     */
    public static class Flash {

        Map<String, String> data = new HashMap<String, String>();
        Map<String, String> out = new HashMap<String, String>();
        static Pattern flashParser = Pattern.compile("\u0000([^:]*):([^\u0000]*)\u0000");

        static Flash restore() {
            try {
                Flash flash = new Flash();
                Http.Cookie cookie = Http.Request.current().cookies.get(COOKIE_PREFIX + "_FLASH");
				String value = null;
                if (cookie != null) {
					value = cookie.value;
				}
				else {
					String psessionid = Scope.Params.current().get("psessionid");
					if(psessionid != null && !psessionid.trim().equals(""))
					{
						value =(String) play.cache.Cache.get(Scope.COOKIE_PREFIX + "_FLASH_"+psessionid);
					}
				
				}
				if(value != null) {
                    String flashData = URLDecoder.decode(value, "utf-8");
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
                Http.Response.current().setCookie(COOKIE_PREFIX + "_FLASH", flashData, null, "/", null, COOKIE_SECURE);
				Http.Cookie cookie = Http.Request.current().cookies.get(COOKIE_PREFIX + "_FLASH");
				if(cookie == null)
				{
					if(flashData==null || flashData.trim().equals(""))
					{
						play.cache.Cache.delete(COOKIE_PREFIX + "_FLASH_"+Scope.Session.current().getId());
					}
					else
					{
						play.cache.Cache.set(COOKIE_PREFIX + "_FLASH_"+Scope.Session.current().getId(),flashData);
					}
					
					
				}

				
            } catch (Exception e) {
                throw new UnexpectedException("Flash serializationProblem", e);
            }
        }        // ThreadLocal access

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

        public void put(String key, Object value) {
            if (value == null) {
                put(key, (String) null);
            }
            put(key, value + "");
        }

        public void now(String key, String value) {
            if (key.contains(":")) {
                throw new IllegalArgumentException("Character ':' is invalid in a flash key.");
            }
            data.put(key, value);
        }

        public void error(String value, Object... args) {
            put("error", String.format(value, args));
        }

        public void success(String value, Object... args) {
            put("success", String.format(value, args));
        }

        public void discard(String key) {
            out.remove(key);
        }

        public void discard() {
            out.clear();
        }

        public void keep(String key) {
            if (data.containsKey(key)) {
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

        public String toString() {
            return data.toString();
        }
    }

    /**
     * Session scope
     */
    public static class Session {

        static Pattern sessionParser = Pattern.compile("\u0000([^:]*):([^\u0000]*)\u0000");

        static Session restore() {
            try {
                Session session = new Session();
                Http.Cookie cookie = Http.Request.current().cookies.get(COOKIE_PREFIX + "_SESSION");
				String value = null;
                if (cookie != null && Play.started) {
					value = cookie.value;
				}
				//donot support cookie
				else if(cookie == null && Play.started) {
					String psessionid = Scope.Params.current().get("psessionid");
					if(psessionid != null && !psessionid.trim().equals(""))
					{
						value =(String) play.cache.Cache.get(Scope.COOKIE_PREFIX + "_SESSION_"+psessionid);
					}
				}
				if(value != null){
                    String sign = value.substring(0, value.indexOf("-"));
                    String data = value.substring(value.indexOf("-") + 1);
                    if (sign.equals(Crypto.sign(data, Play.secretKey.getBytes()))) {
                        String sessionData = URLDecoder.decode(data, "utf-8");
                        Matcher matcher = sessionParser.matcher(sessionData);
                        while (matcher.find()) {
                            session.put(matcher.group(1), matcher.group(2));
                        }
                    }
                    if (COOKIE_EXPIRE != null) {
                        // Verify that the session contains a timestamp, and that it's not expired
                        if (!session.contains("___TS")) {
                            session = new Session();
                        } else {
                            if (Long.parseLong(session.get("___TS")) < System.currentTimeMillis()) {
                                // Session expired
                                session = new Session();
                            }
                        }
                        session.put("___TS", System.currentTimeMillis() + (Time.parseDuration(COOKIE_EXPIRE) * 1000));
                    }
                }
				
                if (!session.contains("___ID")) {
                    session.put("___ID", Codec.UUID());
                }
                return session;
            } catch (Exception e) {
                throw new UnexpectedException("Corrupted HTTP session from " + Http.Request.current().remoteAddress, e);
            }
        }

        Map<String, String> data = new HashMap<String, String>(); // ThreadLocal access
        public static ThreadLocal<Session> current = new ThreadLocal<Session>();

        public static Session current() {
            return current.get();
        }

        public String getId() {
            return data.get("___ID");
        }

        public Map<String, String> all() {
            return data;
        }

        public String getAuthenticityToken() {
            return Crypto.sign(getId());
        }

        void save() {
            try {
                StringBuilder session = new StringBuilder();
                for (String key: data.keySet()) {
                    session.append("\u0000");
                    session.append(key);
                    session.append(":");
                    session.append(data.get(key));
                    session.append("\u0000");
                }
                String sessionData = URLEncoder.encode(session.toString(), "utf-8");
                String sign = Crypto.sign(sessionData, Play.secretKey.getBytes());
				final Http.Request theRequest = Http.Request.current();
				Http.Cookie cookie = theRequest.cookies.get(COOKIE_PREFIX + "_SESSION");
				
				//add  psessionid to GlobalUrlParams
				theRequest.args.put("psessionid",getId());
				if(!theRequest.args.containsKey(play.mvc.Router.GlobalUrlParams))
				{
					theRequest.args.put(play.mvc.Router.GlobalUrlParams,new HashMap<String,Object>());
				}
				((Map) theRequest.args.get(Router.GlobalUrlParams)).put("psessionid", getId());
			
                if (COOKIE_EXPIRE == null) {
                    Http.Response.current().setCookie(COOKIE_PREFIX + "_SESSION", sign + "-" + sessionData, null, "/", null, COOKIE_SECURE, SESSION_HTTPONLY);
					//donot support cookie
					if(cookie == null) {
						play.cache.Cache.set(COOKIE_PREFIX + "_SESSION_"+getId(), sign + "-" + sessionData);
					}
				
                } else {
                    Http.Response.current().setCookie(COOKIE_PREFIX + "_SESSION", sign + "-" + sessionData, null, "/", Time.parseDuration(COOKIE_EXPIRE), COOKIE_SECURE, SESSION_HTTPONLY);
					//donot support cookie
					if(cookie == null) {
						play.cache.Cache.set(COOKIE_PREFIX + "_SESSION_"+getId(), sign + "-" + sessionData,COOKIE_EXPIRE);
					}
                }
				
            } catch (Exception e) {
                throw new UnexpectedException("Session serializationProblem", e);
            }
        }

        public void put(String key, String value) {
            if (key.contains(":")) {
                throw new IllegalArgumentException("Character ':' is invalid in a session key.");
            }
            if(value == null) {
                data.remove(key);
            } else {
                data.put(key, value);
            }
        }

        public void put(String key, Object value) {
            if (value == null) {
                put(key, (String) null);
            }
            put(key, value + "");
        }

        public String get(String key) {
            return data.get(key);
        }

        public boolean remove(String key) {
            return data.remove(key) != null;
        }

        public void remove(String... keys) {
            for (String key : keys) {
                remove(key);
            }
        }

        public void clear() {
            data.clear();
        }

        public boolean contains(String key) {
            return data.containsKey(key);
        }

        @Override
        public String toString() {
            return data.toString();
        }

    }

    /**
     * HTTP params
     */
    public static class Params {
        // ThreadLocal access
        public static ThreadLocal<Params> current = new ThreadLocal<Params>();

        public static Params current() {
            return current.get();
        }

        boolean requestIsParsed;
        private Map<String, String[]> data = new HashMap<String, String[]>();

        public void checkAndParse() {
            if (!requestIsParsed) {
                Http.Request request = Http.Request.current();
                String contentType = request.contentType;
                if (contentType != null) {
                    DataParser dataParser = DataParser.parsers.get(contentType);
                    if (dataParser != null) {
                        _mergeWith(dataParser.parse(request.body));
                    }
                }
                try {
                    request.body.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                requestIsParsed = true;
            }
        }

        public void put(String key, String value) {
            data.put(key, new String[]{value});
        }

        public void put(String key, String[] values) {
            data.put(key, values);
        }

        public void remove(String key) {
            data.remove(key);
        }

        public String get(String key) {
            if (!_contains(key)) {
                checkAndParse();
            }
            if (data.containsKey(key)) {
                return data.get(key)[0];
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        public <T> T get(String key, Class<T> type) {
            try {
                // TODO: This is used by the test, but this is not the most convenient.
                return (T) Binder.directBind(key, null, get(key), type);
            } catch (Exception e) {
                Validation.addError(key, "validation.invalid");
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T get(Annotation[] annotations, String key, Class<T> type) {
            try {
                return (T) Binder.directBind(key, annotations, get(key), type);
            } catch (Exception e) {
                Validation.addError(key, "validation.invalid");
                return null;
            }
        }

        public boolean _contains(String key) {
            return data.containsKey(key);
        }

        public String[] getAll(String key) {
            if (!_contains(key)) {
                checkAndParse();
            }
            return data.get(key);
        }

        public Map<String, String[]> all() {
            checkAndParse();
            return data;
        }

        public Map<String, String[]> sub(String prefix) {
            checkAndParse();
            Map<String, String[]> result = new HashMap<String, String[]>();
            for (String key : data.keySet()) {
                if (key.startsWith(prefix + ".")) {
                    result.put(key.substring(prefix.length() + 1), data.get(key));
                }
            }
            return result;
        }

        public Map<String, String> allSimple() {
            checkAndParse();
            Map<String, String> result = new HashMap<String, String>();
            for (String key : data.keySet()) {
                result.put(key, data.get(key)[0]);
            }
            return result;
        }

        void _mergeWith(Map<String, String[]> map) {
            for (Map.Entry<String, String[]> entry : map.entrySet()) {
                Utils.Maps.mergeValueInMap(data, entry.getKey(), entry.getValue());
            }
        }

        void __mergeWith(Map<String, String> map) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                Utils.Maps.mergeValueInMap(data, entry.getKey(), entry.getValue());
            }
        }

        public String urlEncode() {
            checkAndParse();
            StringBuffer ue = new StringBuffer();
            for (String key : data.keySet()) {
                if (key.equals("body")) {
                    continue;
                }
                String[] values = data.get(key);
                for (String value : values) {
                    try {
                        ue.append(URLEncoder.encode(key, "utf-8")).append("=").append(URLEncoder.encode(value, "utf-8")).append("&");
                    } catch (Exception e) {
                        Logger.error(e, "Error (utf-8 ?)");
                    }
                }
            }
            return ue.toString();
        }

        public void flash(String... params) {
            if (params.length == 0) {
                for (String key : all().keySet()) {
                    if (data.get(key).length > 1) {
                        StringBuilder sb = new StringBuilder();
                        boolean coma = false;
                        for (String d : data.get(key)) {
                            if (coma) sb.append(",");
                            sb.append(d);
                            coma = true;
                        }
                        Flash.current().put(key, sb.toString());
                    } else {
                        Flash.current().put(key, get(key));
                    }
                }
            } else {
                for (String key : params) {
                    if (data.get(key).length > 1) {
                        StringBuilder sb = new StringBuilder();
                        boolean coma = false;
                        for (String d : data.get(key)) {
                            if (coma) sb.append(",");
                            sb.append(d);
                            coma = true;
                        }
                        Flash.current().put(key, sb.toString());
                    } else {
                        Flash.current().put(key, get(key));
                    }
                }
            }
        }

        @Override
        public String toString() {
            return data.toString();
        }

    }

    /**
     * Render args (used in template rendering)
     */
    public static class RenderArgs {

        public Map<String, Object> data = new HashMap<String, Object>();        // ThreadLocal access
        public static ThreadLocal<RenderArgs> current = new ThreadLocal<RenderArgs>();

        public static RenderArgs current() {
            return current.get();
        }

        public void put(String key, Object arg) {
            this.data.put(key, arg);
        }

        public Object get(String key) {
            return data.get(key);
        }

        @SuppressWarnings("unchecked")
        public <T> T get(String key, Class<T> clazz) {
            return (T) this.get(key);
        }

        @Override
        public String toString() {
            return data.toString();
        }

    }

    /**
     * Routes args (used in reserve routing)
     */
    public static class RouteArgs {

        public Map<String, Object> data = new HashMap<String, Object>();        // ThreadLocal access
        public static ThreadLocal<RouteArgs> current = new ThreadLocal<RouteArgs>();

        public static RouteArgs current() {
            return current.get();
        }

        public void put(String key, Object arg) {
            this.data.put(key, arg);
        }

        public Object get(String key) {
            return data.get(key);
        }

        @Override
        public String toString() {
            return data.toString();
        }

    }

}
