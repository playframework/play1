package play.mvc;

import play.Logger;
import play.Play;
import play.data.binding.Binder;
import play.data.binding.ParamNode;
import play.data.binding.RootParamNode;
import play.data.parsing.DataParser;
import play.data.parsing.DataParsers;
import play.data.validation.Validation;
import play.exceptions.UnexpectedException;
import play.i18n.Messages;
import play.inject.Injector;
import play.libs.Codec;
import play.libs.Crypto;
import play.utils.Utils;

import java.lang.annotation.Annotation;
import java.net.URLEncoder;
import java.util.*;

/**
 * All application Scopes
 */
public class Scope {

    public static final String COOKIE_PREFIX = Play.configuration.getProperty("application.session.cookie", "PLAY");
    public static final boolean COOKIE_SECURE = Play.configuration.getProperty("application.session.secure", "false").toLowerCase()
            .equals("true");
    public static final String COOKIE_EXPIRE = Play.configuration.getProperty("application.session.maxAge");
    public static final boolean SESSION_HTTPONLY = Play.configuration.getProperty("application.session.httpOnly", "false").toLowerCase()
            .equals("true");
    public static final boolean SESSION_SEND_ONLY_IF_CHANGED = Play.configuration
            .getProperty("application.session.sendOnlyIfChanged", "false").toLowerCase().equals("true");

    public static SessionStore sessionStore = createSessionStore();

    private static SessionStore createSessionStore() {
        String sessionStoreClass = Play.configuration.getProperty("application.session.storeClass");
        if (sessionStoreClass == null) {
            return Injector.getBeanOfType(CookieSessionStore.class);
        }

        try {
            Logger.info("Storing sessions using " + sessionStoreClass);
            return (SessionStore) Injector.getBeanOfType(sessionStoreClass);
        }
        catch (Exception e) {
            throw new UnexpectedException("Cannot create instance of " + sessionStoreClass, e);
        }
    }

    /**
     * Flash scope
     */
    public static class Flash {

        Map<String, String> data = new HashMap<>();
        Map<String, String> out = new HashMap<>();

        public static Flash restore() {
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
                if (Http.Request.current().cookies.containsKey(COOKIE_PREFIX + "_FLASH") || !SESSION_SEND_ONLY_IF_CHANGED) {
                    Http.Response.current().setCookie(COOKIE_PREFIX + "_FLASH", "", null, "/", 0, COOKIE_SECURE, SESSION_HTTPONLY);
                }
                return;
            }
            try {
                String flashData = CookieDataCodec.encode(out);
                Http.Response.current().setCookie(COOKIE_PREFIX + "_FLASH", flashData, null, "/", null, COOKIE_SECURE, SESSION_HTTPONLY);
            } catch (Exception e) {
                throw new UnexpectedException("Flash serializationProblem", e);
            }
        } // ThreadLocal access

        public static final ThreadLocal<Flash> current = new ThreadLocal<>();

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
            put("error", Messages.get(value, args));
        }

        public void success(String value, Object... args) {
            put("success", Messages.get(value, args));
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

        public static Session restore() {
            return sessionStore.restore();
        }

        Map<String, String> data = new HashMap<>(); // ThreadLocal access
        boolean changed = false;
        public static final ThreadLocal<Session> current = new ThreadLocal<>();

        public static Session current() {
            return current.get();
        }

        public String getId() {
            if (!data.containsKey(ID_KEY)) {
                this.put(ID_KEY, Codec.UUID());
            }
            return data.get(ID_KEY);

        }

        public Map<String, String> all() {
            return data;
        }

        public String getAuthenticityToken() {
            if (!data.containsKey(AT_KEY)) {
                this.put(AT_KEY, Crypto.sign(Codec.UUID()));
            }
            return data.get(AT_KEY);
        }

        void change() {
            changed = true;
        }

        void save() {
            sessionStore.save(this);
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
            } else {
                put(key, value.toString());
            }
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
         * Returns true if the session is empty, e.g. does not contain anything else than the timestamp
         * 
         * @return true if the session is empty, otherwise false
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

        public static final ThreadLocal<Params> current = new ThreadLocal<>();

        public static Params current() {
            return current.get();
        }

        boolean requestIsParsed;
        public Map<String, String[]> data = new LinkedHashMap<>();

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
                if (request == null) {
                    throw new UnexpectedException("Current request undefined");
                } else {
                    String contentType = request.contentType;
                    if (contentType != null) {
                        DataParser dataParser = DataParsers.forContentType(contentType);
                        if (dataParser != null) {
                            _mergeWith(dataParser.parse(request.body));
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
        }

        public void put(String key, String value) {
            checkAndParse();
            data.put(key, new String[] { value });
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

        public void removeStartWith(String prefix) {
            checkAndParse();
            Iterator<Map.Entry<String, String[]>> iterator = data.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String[]> entry = iterator.next();
                if (entry.getKey().startsWith(prefix)) {
                    iterator.remove();
                }
            }
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
                // TODO: This is used by the test, but this is not the most
                // convenient.
                return (T) Binder.bind(getRootParamNode(), key, type, type, null);
            } catch (RuntimeException e) {
                Logger.error(e, "Failed to get %s of type %s", key, type);
                Validation.addError(key, "validation.invalid");
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T get(Annotation[] annotations, String key, Class<T> type) {
            try {
                return (T) Binder.directBind(annotations, get(key), type, null);
            } catch (Exception e) {
                Logger.error(e, "Failed to get %s of type %s", key, type);
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
            Map<String, String[]> result = new LinkedHashMap<>();
            for (String key : data.keySet()) {
                if (key.startsWith(prefix + ".")) {
                    result.put(key.substring(prefix.length() + 1), data.get(key));
                }
            }
            return result;
        }

        public Map<String, String> allSimple() {
            checkAndParse();
            Map<String, String> result = new HashMap<>();
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

        public Map<String, Object> data = new HashMap<>(); // ThreadLocal access
        public static final ThreadLocal<RenderArgs> current = new ThreadLocal<>();

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

        public Map<String, Object> data = new HashMap<>(); // ThreadLocal access
        public static final ThreadLocal<RouteArgs> current = new ThreadLocal<>();

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
