package play.i18n;

import play.Logger;
import play.Play;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope;

import java.util.*;

/**
 * Language support
 */
public class Lang {

    static final ThreadLocal<String> current = new ThreadLocal<>();

    private static Map<String, Locale> cache = new HashMap<>();
    
    /**
     * Retrieve the current language or null
     *
     * @return The current language (fr, ja, it ...) or null
     */
    public static String get() {
        String locale = current.get();
        if (locale == null) {
            // don't have current locale for this request - must try to resolve it
            Http.Request currentRequest = Http.Request.current();
            if (currentRequest != null) {
                // we have a current request - lets try to resolve language from it
                resolveFrom(currentRequest);
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
     *
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
     *
     * @param locale (e.g. "fr", "ja", "it", "en_ca", "fr_be", ...)
     */
    public static void change(String locale) {
        String closestLocale = findClosestMatch(Collections.singleton(locale));
        if (closestLocale == null) {
            // Give up
            return;
        }
        if (set(closestLocale)) {
            Response response = Response.current();
            if (response != null) {
                // We have a current response in scope - set the language-cookie to store the selected language for the next requests
                response.setCookie(Play.configuration.getProperty("application.lang.cookie", "PLAY_LANG"), locale, null, "/", null, Scope.COOKIE_SECURE);
            }
        }

    }

    /**
     * Given a set of desired locales, searches the set of locales supported by this Play! application and returns the closest match.
     *
     * @param desiredLocales a collection of desired locales. If the collection is ordered, earlier locales are preferred over later ones.
     *                       Locales should be of the form "[language]_[country" or "[language]", e.g. "en_CA" or "en".
     *                       The locale strings are case insensitive (e.g. "EN_CA" is considered the same as "en_ca").
     *                       Locales can also be of the form "[language]-[country", e.g. "en-CA" or "en".
     *                       They are still case insensitive, though (e.g. "EN-CA" is considered the same as "en-ca").
     * @return the closest matching locale. If no closest match for a language/country is found, null is returned
     */
    private static String findClosestMatch(Collection<String> desiredLocales) {
        ArrayList<String> cleanLocales = new ArrayList<>(desiredLocales.size());
        //look for an exact match
        for (String a : desiredLocales) {
            a = a.replace("-", "_");
            cleanLocales.add(a);
            for (String locale : Play.langs) {
                if (locale.equalsIgnoreCase(a)) {
                    return locale;
                }
            }
        }
        // Exact match not found, try language-only match.
        for (String a : cleanLocales) {
            int splitPos = a.indexOf("_");
            if (splitPos > 0) {
                a = a.substring(0, splitPos);
            }
            for (String locale : Play.langs) {
                String langOnlyLocale;
                int localeSplitPos = locale.indexOf("_");
                if (localeSplitPos > 0) {
                    langOnlyLocale = locale.substring(0, localeSplitPos);
                } else {
                    langOnlyLocale = locale;
                }
                if (langOnlyLocale.equalsIgnoreCase(a)) {
                    return locale;
                }
            }
        }

        // We did not find a anything
        return null;
    }

    /**
     * Guess the language for current request in the following order:
     * <ol>
     * <li>if a <b>PLAY_LANG</b> cookie is set, use this value</li>
     * <li>if <b>Accept-Language</b> header is set, use it only if the Play! application allows it.<br/>supported language may be defined in application configuration, eg : <em>play.langs=fr,en,de)</em></li>
     * <li>otherwise, server's locale language is assumed
     * </ol>
     *
     * @param request current request
     */
    private static void resolveFrom(Request request) {
        // Check a cookie
        String cn = Play.configuration.getProperty("application.lang.cookie", "PLAY_LANG");
        if (request.cookies.containsKey(cn)) {
            String localeFromCookie = request.cookies.get(cn).value;
            if (localeFromCookie != null && localeFromCookie.trim().length() > 0) {
                if (set(localeFromCookie)) {
                    // we're using locale from cookie
                    return;
                }
                // could not use locale from cookie - clear the locale-cookie
                Response.current().setCookie(cn, "", null, "/", null, Scope.COOKIE_SECURE);

            }

        }
        String closestLocaleMatch = findClosestMatch(request.acceptLanguage());
        if (closestLocaleMatch != null) {
            set(closestLocaleMatch);
        } else {
            // Did not find anything - use default
            setDefaultLocale();
        }

    }

    public static void setDefaultLocale() {
        if (Play.langs.isEmpty()) {
            set("");
        } else {
            set(Play.langs.get(0));
        }
    }

    /**
     * @return the default locale if the Locale cannot be found otherwise the locale
     * associated to the current Lang.
     */
    public static Locale getLocale() {
        return getLocaleOrDefault(get());
    }

    public static Locale getLocaleOrDefault(String localeStr) {
        Locale locale = getLocale(localeStr);
        if (locale != null) {
            return locale;
        }
        return Locale.getDefault();
    }

    public static Locale getLocale(String localeStr) {
        if (localeStr == null) {
            return null;
        }

        Locale result = cache.get(localeStr);
        
        if (result == null) {
            result = findLocale(localeStr);
            cache.put(localeStr, result);
        }
        
        return result;
    }

    private static Locale findLocale(String localeStr) {
        String lang = localeStr;
        int splitPos = lang.indexOf("_");
        if (splitPos > 0) {
            lang = lang.substring(0, splitPos);
        }

        Locale result = null;
        for (Locale locale : Locale.getAvailableLocales()) {
            if (locale.toString().equalsIgnoreCase(localeStr)) {
                return locale;
            } else if (locale.getLanguage().equalsIgnoreCase(lang)) {
                result = locale;
            }
        }
        return result;
    }
}
