package play.mvc;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import play.Logger;
import play.Play;
import play.data.binding.Binder;
import play.data.binding.ParamNode;
import play.data.binding.RootParamNode;
import play.data.parsing.DataParser;
import play.data.parsing.TextParser;
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
    public static final boolean SESSION_SEND_ONLY_IF_CHANGED = Play.configuration.getProperty("application.session.sendOnlyIfChanged", "false").toLowerCase().equals("true");

    /**
     * Flash scope
     */
    public static class Flash {

        Map<String, String> data = new HashMap<String, String>();
        Map<String, String> out = new HashMap<String, String>();

        static Flash restore() {
            try {
                Flash flash = new Flash();
                Http.Cookie cookie = Http.Request.current().cookies.get(COOKIE_PREFIX + "_FLASH");
                if (cookie != null) {
                    CookieDataCodec.decode(flash.data, cookie.value);
                }
                return flash;
            } catch (Exception e) {
                throw new UnexpectedException("Flash corrupted", e);
            }
        }

        void save() {
            if (Http.Response.current() == null) {
                // Some request like WebSocket don't have any response
                return;
            }
            if (out.isEmpty()) {
                if(Http.Request.current().cookies.containsKey(COOKIE_PREFIX + "_FLASH") || !SESSION_SEND_ONLY_IF_CHANGED) {
                    Http.Response.current().setCookie(COOKIE_PREFIX + "_FLASH", "", null, "/", 0, COOKIE_SECURE);
                }
                return;
            }
            try {
                String flashData = CookieDataCodec.encode(out);
                Http.Response.current().setCookie(COOKIE_PREFIX + "_FLASH", flashData, null, "/", null, COOKIE_SECURE);
            } catch (Exception e) {
                throw new UnexpectedException("Flash serializationProblem", e);
            }
        }        // ThreadLocal access
        public static ThreadLocal<Flash> current = new ThreadLocal<Flash>();

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

        @Override
        public String toString() {
            return data.toString();
        }
    }

    /**
     * Session scope
     */
    public static class Session {

        static final String AT_KEY = "___AT";
        static final String ID_KEY = "___ID";
        static final String TS_KEY = "___TS";

        static Session restore() {
            try {
                Session session = new Session();
                Http.Cookie cookie = Http.Request.current().cookies.get(COOKIE_PREFIX + "_SESSION");
				final int duration = Time.parseDuration(COOKIE_EXPIRE) ;
				final long expiration = (duration * 1000l);

                if (cookie != null && Play.started && cookie.value != null && !cookie.value.trim().equals("")) {
                    String value = cookie.value;
				 	int firstDashIndex = value.indexOf("-");
				    if(firstDashIndex > -1) {
                    	String sign = value.substring(0, firstDashIndex);
                    	String data = value.substring(firstDashIndex + 1);
                    	if (CookieDataCodec.safeEquals(sign, Crypto.sign(data, Play.secretKey.getBytes()))) {
                            CookieDataCodec.decode(session.data, data);
                    	}
					} 
                    if (COOKIE_EXPIRE != null) {
                        // Verify that the session contains a timestamp, and that it's not expired
					    if (!session.contains(TS_KEY)) {
                            session = new Session();
                        } else {
					        if ((Long.parseLong(session.get(TS_KEY))) < System.currentTimeMillis()) {
                                // Session expired
                                session = new Session();
                            }
                        }
					    session.put(TS_KEY, System.currentTimeMillis() + expiration);
                    } else {
                        // Just restored. Nothing changed. No cookie-expire.
                        session.changed = false;
                    }
                } else {
                    // no previous cookie to restore; but we may have to set the timestamp in the new cookie
			        if (COOKIE_EXPIRE != null) {	
				        session.put(TS_KEY, (System.currentTimeMillis() + expiration));
                    }
                }

                return session;
            } catch (Exception e) {
                throw new UnexpectedException("Corrupted HTTP session from " + Http.Request.current().remoteAddress, e);
            }
        }
        Map<String, String> data = new HashMap<String, String>(); // ThreadLocal access
        boolean changed = false;
        public static ThreadLocal<Session> current = new ThreadLocal<Session>();

        public static Session current() {
            return current.get();
        }

        public String getId() {
            if (!data.containsKey(ID_KEY)) {
                data.put(ID_KEY, Codec.UUID());
            }
            return data.get(ID_KEY);

        }

        public Map<String, String> all() {
            return data;
        }

        public String getAuthenticityToken() {
            if (!data.containsKey(AT_KEY)) {
                data.put(AT_KEY, Crypto.sign(UUID.randomUUID().toString()));
            }
            return data.get(AT_KEY);
        }

        void change() {
            changed = true;
        }

        void save() {
            if (Http.Response.current() == null) {
                // Some request like WebSocket don't have any response
                return;
            }
            if(!changed && SESSION_SEND_ONLY_IF_CHANGED && COOKIE_EXPIRE == null) {
                // Nothing changed and no cookie-expire, consequently send nothing back.
                return;
            }
            if (isEmpty()) {
                // The session is empty: delete the cookie
                if(Http.Request.current().cookies.containsKey(COOKIE_PREFIX + "_SESSION") || !SESSION_SEND_ONLY_IF_CHANGED) {
                    Http.Response.current().setCookie(COOKIE_PREFIX + "_SESSION", "", null, "/", 0, COOKIE_SECURE, SESSION_HTTPONLY);
                }
                return;
            }
            try {
                String sessionData = CookieDataCodec.encode(data);
                String sign = Crypto.sign(sessionData, Play.secretKey.getBytes());
                if (COOKIE_EXPIRE == null) {
                    Http.Response.current().setCookie(COOKIE_PREFIX + "_SESSION", sign + "-" + sessionData, null, "/", null, COOKIE_SECURE, SESSION_HTTPONLY);
                } else {
                    Http.Response.current().setCookie(COOKIE_PREFIX + "_SESSION", sign + "-" + sessionData, null, "/", Time.parseDuration(COOKIE_EXPIRE), COOKIE_SECURE, SESSION_HTTPONLY);
                }
            } catch (Exception e) {
                throw new UnexpectedException("Session serializationProblem", e);
            }
        }

        public void put(String key, String value) {
            if (key.contains(":")) {
                throw new IllegalArgumentException("Character ':' is invalid in a session key.");
            }
            change();
            if (value == null) {
                data.remove(key);
            } else {
                data.put(key, value);
            }
        }

        public void put(String key, Object value) {
            change();
            if (value == null) {
                put(key, (String) null);
            }
            put(key, value + "");
        }

        public String get(String key) {
            return data.get(key);
        }

        public boolean remove(String key) {
            change();
            return data.remove(key) != null;
        }

        public void remove(String... keys) {
            for (String key : keys) {
                remove(key);
            }
        }

        public void clear() {
            change();
            data.clear();
        }

        /**
         * Returns true if the session is empty,
         * e.g. does not contain anything else than the timestamp
         */
        public boolean isEmpty() {
            for (String key : data.keySet()) {
                if (!TS_KEY.equals(key)) {
                    return false;
                }
            }
            return true;
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
        public Map<String, String[]> data = new HashMap<String, String[]>();

        boolean rootParamsNodeIsGenerated = false;
        private RootParamNode rootParamNode = null;

        public RootParamNode getRootParamNode() {
            checkAndParse();
            if (!rootParamsNodeIsGenerated) {
                rootParamNode = ParamNode.convert(data);
                rootParamsNodeIsGenerated = true;
            }
            return rootParamNode;
        }

        public RootParamNode getRootParamNodeFromRequest() {
            return ParamNode.convert(data);
        }

        public void checkAndParse() {
            if (!requestIsParsed) {
                Http.Request request = Http.Request.current();
                String contentType = request.contentType;
                if (contentType != null) {
                    DataParser dataParser = DataParser.parsers.get(contentType);
                    if (dataParser != null) {
                        _mergeWith(dataParser.parse(request.body));
                    } else {
                        if (contentType.startsWith("text/")) {
                            _mergeWith(new TextParser().parse(request.body));
                        }
                    }
                }
                try {
                    request.body.close();
                } catch (Exception e) {
                    //
                }
                requestIsParsed = true;
            }
        }

        public void put(String key, String value) {
            checkAndParse();
            data.put(key, new String[]{value});
            // make sure rootsParamsNode is regenerated if needed
            rootParamsNodeIsGenerated = false;
        }

        public void put(String key, String[] values) {
            checkAndParse();
            data.put(key, values);
            // make sure rootsParamsNode is regenerated if needed
            rootParamsNodeIsGenerated = false;
        }

        public void remove(String key) {
            checkAndParse();
            data.remove(key);
            // make sure rootsParamsNode is regenerated if needed
            rootParamsNodeIsGenerated = false;
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
                checkAndParse();
                // TODO: This is used by the test, but this is not the most convenient.
                return (T) Binder.bind(getRootParamNode(), key, type, type, null);
            } catch (Exception e) {
                Validation.addError(key, "validation.invalid");
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T get(Annotation[] annotations, String key, Class<T> type) {
            try {
                return (T) Binder.directBind(annotations, get(key), type, null);
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
            String encoding = Http.Response.current().encoding;
            StringBuilder ue = new StringBuilder();
            for (String key : data.keySet()) {
                if (key.equals("body")) {
                    continue;
                }
                String[] values = data.get(key);
                for (String value : values) {
                    try {
                        ue.append(URLEncoder.encode(key, encoding)).append("=").append(URLEncoder.encode(value, encoding)).append("&");
                    } catch (Exception e) {
                        Logger.error(e, "Error (encoding ?)");
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
                            if (coma) {
                                sb.append(",");
                            }
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
                            if (coma) {
                                sb.append(",");
                            }
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
