package play.i18n;

import java.util.Collections;
import java.util.Locale;
import play.Logger;
import play.Play;
import play.mvc.Http;
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
        String locale = current.get();
        if (locale == null) {
            // don't have current locale for this request - must try to resolve it
            Http.Request currentRequest = Http.Request.current();
            if (currentRequest!=null) {
                // we have a current request - lets try to resolve language from it
                resolvefrom( currentRequest );
            } else {
                // don't have current request - just use default
                setDefaultLocale();
            }
            // get the picked locale
            locale = current.get();
        }
        return locale;
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
     * Clears the current language - This wil trigger resolving language from request
     * if not manually set.
     */
    public static void clear() {
        current.remove();
    }


    /**
     * Change language for next requests 
     * @param locale (e.g. "fr", "ja", "it", "en_ca", "fr_be", ...)
     */
    public static void change(String locale) {
        String closestLocale = findClosestMatch(Collections.singleton(locale));
        boolean success = set(closestLocale);
        //findClosestMatch should always return a valid locale, so success should always be true.
        Response.current().setCookie(Play.configuration.getProperty("application.lang.cookie", "PLAY_LANG"), locale);
    }

    /**
     * Given a set of desired locales, searches the set of locales supported by this Play! application and returns the closest match.
     *
     * @param desiredLocales a collection of desired locales. If the collection is ordered, earlier locales are preferred over later ones. Locales should be of the form "[language]_[country" or "[language]", e.g. "en_CA" or "en". The locale strings are case insensitive (e.g. "EN_CA" is considered the same as "en_ca").
     * @return the closest matching locale. If no locales are defined in this Play! application, the empty string is returned.
     */
    private static String findClosestMatch(Iterable<String> desiredLocales) {
        //look for an exact match
        for (String a: desiredLocales) {
            for (String locale: Play.langs) {
                if (locale.equalsIgnoreCase(a)) {
                    return locale;
                }
            }
        }
        // Exact match not found, try language-only match.
        for (String a: desiredLocales) {
            if (a.indexOf("_") > 0) {
                a = a.substring(0, a.indexOf("_"));
            }
            for (String locale: Play.langs) {
                String langOnlyLocale;
                if (locale.indexOf("_") > 0) {
                    langOnlyLocale = locale.substring(0, locale.indexOf("_"));
                } else {
                    langOnlyLocale = locale;
                }
                if (langOnlyLocale.equalsIgnoreCase(a)) {
                    return locale;
                }
            }
        }
        //No matches found. Return default locale
        if (Play.langs.isEmpty()) {
            return "";
        } else {
            return Play.langs.get(0);
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
    private static void resolvefrom(Request request) {
        // Check a cookie
        String cn = Play.configuration.getProperty("application.lang.cookie", "PLAY_LANG");
        if (request.cookies.containsKey(cn)) {
            String localeFromCookie = request.cookies.get(cn).value;
            if (localeFromCookie != null && localeFromCookie.trim().length()>0) {
                if (set(localeFromCookie)) {
                    // we're using locale from cookie
                    return;
                }
                // could not use locale from cookie - clear the locale-cookie
                Response.current().setCookie(cn, "");

            }

        }
        String closestLocaleMatch = findClosestMatch(request.acceptLanguage());
        set(closestLocaleMatch);
    }

    public static void setDefaultLocale() {
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
