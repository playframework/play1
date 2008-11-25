package play.i18n;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * I18n Helper
 * <p>
 * translation are defined as properties in /conf/messages.<em>locale</em> files
 * with locale being the i18n country code fr, en, fr_FR
 * 
 * <pre>
 * # /conf/messages.fr
 * hello=Bonjour, %s !
 * </pre>
 * <code>
 * Messages.get( "hello", "World"); // => "Bonjour, World !"
 * </code>
 * 
 */
public class Messages {

    static protected Properties defaults;

    static protected Map<String, Properties> locales = new HashMap<String, Properties>();

    /**
     * Given a message code, translate it using current locale.
     * Notice that if the message can't be found, the string <em>!code!</em> is returned.
     * 
     * @param key the message code
     * @param args optional message format arguments
     * @return translated message
     */
    public static String get(Object key, Object... args) {
        String value = null;
        if( key == null ) {
            return "";
        }
        if (locales.containsKey(Lang.get())) {
            value = locales.get(Lang.get()).getProperty(key.toString());
        }
        if (value == null) {
            value = defaults.getProperty(key.toString());
        }
        if (value == null) {
            value = "!" + key + "!";
        }
        return String.format(value, args);
    }

    /**
     * return all messages for a locale
     * @param locale the locale code eg. fr, fr_FR
     * @return messages as a {@link java.util.Properties java.util.Properties}
     */
    public static Properties all(String locale) {
        return locales.get(locale);
    }
}
