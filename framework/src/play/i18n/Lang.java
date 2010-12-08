package play.i18n;

import java.util.Locale;
import play.Logger;
import play.Play;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

/**
 * Language support
 */
public class Lang {

    public static ThreadLocal<String> current = new ThreadLocal<String>();

    /**
     * Retrieve the current language or null
     * @return The current language (fr, ja, it ...) or null
     */
    public static String get() {
        return current.get();
    }

    /**
     * Force the current language
     * @param locale (fr, ja, it ...)
     * @return false if the language is not supported by the application
     */
    public static boolean set(String locale) {
        if (locale.equals("") || Play.langs.contains(locale)) {
            current.set(locale);
            return true;
        } else {
            Logger.warn("Locale %s is not defined in your application.conf", locale);
            return false;
        }
    }

    /**
     * Change language for next requests 
     * @param locale (fr, ja, it ...)
     */
    public static void change(String locale) {
        if (set(locale)) {
            Response.current().setCookie(Play.configuration.getProperty("application.lang.cookie", "PLAY_LANG"), locale);
        }
    }

    /**
     * Guess the language for current request in the following order:
     * <ol>
     * <li>if a <b>PLAY_LANG</b> cookie is set, use this value</li>
     * <li>if <b>Accept-Language</b> header is set, use it only if the Play! application allows it.<br/>supported language may be defined in application configuration, eg : <em>play.langs=fr,en,de)</em></li>
     * <li>otherwise, server's locale language is assumed
     * </ol>
     * @param request
     */
    public static void resolvefrom(Request request) {
        // Check a cookie
        String cn = Play.configuration.getProperty("application.lang.cookie", "PLAY_LANG");
        if (request.cookies.containsKey(cn)) {
            if (!set(request.cookies.get(cn).value)) {
                Response.current().setCookie(cn, "");
            }
            return;
        }
        // Try from accept-language - look for an exact match
        for (String a: request.acceptLanguage()) {
            a = a.replace("-", "_").toLowerCase();
            for (String locale: Play.langs) {
                if (locale.toLowerCase().equals(a)) {
                    set(locale);
                    return;
                }
            }
        }
        // now see if we have a country-only match
        for (String a: request.acceptLanguage()) {
            if (a.indexOf("-") > 0) {
                a = a.substring(0, a.indexOf("-"));
            }
            for (String locale: Play.langs) {
                if (locale.equals(a)) {
                    set(locale);
                    return;
                }
            }
        }
        // Use default
        if (Play.langs.isEmpty()) {
            set("");
        } else {
            set(Play.langs.get(0));
        }
    }

    /**
     *
     * @return the default locale if the Locale cannot be found otherwise the locale
     * associated to the current Lang.
     */
    public static Locale getLocale() {
        String lang = get();
        Locale locale = getLocale(lang);
        if (locale != null) {
            return locale;
        }
        return Locale.getDefault();
    }

     public static Locale getLocale(String lang) {
        for (Locale locale : Locale.getAvailableLocales()) {
            if (locale.getLanguage().equals(lang)) {
                return locale;
            }
        }
        return null;
    }

}
