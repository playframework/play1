package play.i18n;

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
        setDefaultLocale();
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
        return getLocaleOrDefault(get());
    }

    public static Locale getLocaleOrDefault(String lang) {
        Locale locale = getLocale(lang);
        if (locale != null) {
            return locale;
        }
        return Locale.getDefault(); 
    }
    
    public static Locale getLocale(String lang) {
        if(lang == null) {
            return null;
        }

        String country = "";
        String language = "";

        // Only language (eg. fr)
        if(lang.length() == 2) {
            language = lang.toLowerCase();
        }

        // language and country (eg. fr_FR)
        if(lang.length() == 5) {
            if(lang.charAt(2) != '_') {
                return null;
            }

            language = lang.substring(0, 2).toLowerCase();
            country = lang.substring(3, 5).toUpperCase();
        }

        for(Locale locale : Locale.getAvailableLocales()) {
            if(locale.getLanguage().equals(language)
                && locale.getCountry().equals(country)) {
                return locale;
            }
        }

        return null;
    }
}
