package play.mvc;

import play.Play;
import play.exceptions.UnexpectedException;
import play.libs.Crypto;
import play.libs.Time;

import static play.mvc.Scope.*;
import static play.mvc.Scope.Session.TS_KEY;

/**
 * Default session store implementation that stores signed data in a cookie
 */
public class CookieSessionStore implements SessionStore {

    @Override
    public Session restore() {
        try {
            Session session = new Session();
            Http.Cookie cookie = Http.Request.current().cookies.get(COOKIE_PREFIX + "_SESSION");
            int duration = Time.parseDuration(COOKIE_EXPIRE);
            long expiration = (duration * 1000l);

            if (cookie != null && Play.started && cookie.value != null && !cookie.value.trim().equals("")) {
                String value = cookie.value;
                int firstDashIndex = value.indexOf("-");
                if (firstDashIndex > -1) {
                    String sign = value.substring(0, firstDashIndex);
                    String data = value.substring(firstDashIndex + 1);
                    if (CookieDataCodec.safeEquals(sign, Crypto.sign(data, Play.secretKey.getBytes()))) {
                        CookieDataCodec.decode(session.data, data);
                    }
                }
                if (COOKIE_EXPIRE != null) {
                    // Verify that the session contains a timestamp, and
                    // that it's not expired
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
                // no previous cookie to restore; but we may have to set the
                // timestamp in the new cookie
                if (COOKIE_EXPIRE != null) {
                    session.put(TS_KEY, (System.currentTimeMillis() + expiration));
                }
            }

            return session;
        } catch (Exception e) {
            throw new UnexpectedException("Corrupted HTTP session from " + Http.Request.current().remoteAddress, e);
        }
    }

    @Override
    public void save(Session session) {
        if (Http.Response.current() == null) {
            // Some request like WebSocket don't have any response
            return;
        }
        if (!session.changed && SESSION_SEND_ONLY_IF_CHANGED && COOKIE_EXPIRE == null) {
            // Nothing changed and no cookie-expire, consequently send
            // nothing back.
            return;
        }
        if (session.isEmpty()) {
            // The session is empty: delete the cookie
            if (Http.Request.current().cookies.containsKey(COOKIE_PREFIX + "_SESSION") || !SESSION_SEND_ONLY_IF_CHANGED) {
                Http.Response.current().setCookie(COOKIE_PREFIX + "_SESSION", "", null, "/", 0, COOKIE_SECURE, SESSION_HTTPONLY);
            }
            return;
        }
        try {
            String sessionData = CookieDataCodec.encode(session.data);
            String sign = Crypto.sign(sessionData, Play.secretKey.getBytes());
            if (COOKIE_EXPIRE == null) {
                Http.Response.current().setCookie(COOKIE_PREFIX + "_SESSION", sign + "-" + sessionData, null, "/", null, COOKIE_SECURE,
                        SESSION_HTTPONLY);
            } else {
                Http.Response.current().setCookie(COOKIE_PREFIX + "_SESSION", sign + "-" + sessionData, null, "/",
                        Time.parseDuration(COOKIE_EXPIRE), COOKIE_SECURE, SESSION_HTTPONLY);
            }
        } catch (Exception e) {
            throw new UnexpectedException("Session serializationProblem", e);
        }
    }
}
