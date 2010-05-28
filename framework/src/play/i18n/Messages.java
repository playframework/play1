package play.i18n;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import play.data.binding.map.OldBinder;

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

    static public Properties defaults;

    static public Map<String, Properties> locales = new HashMap<String, Properties>();
    
    static Pattern recursive = Pattern.compile("&\\{(.*?)\\}");

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
            value = key.toString();
        }
        String message = String.format(value, coolStuff(value, args));
        Matcher matcher = recursive.matcher(message);
        StringBuffer sb = new StringBuffer();
        while(matcher.find()) {
            matcher.appendReplacement(sb, get(matcher.group(1)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
    
    static Pattern formatterPattern = Pattern.compile("%((\\d+)\\$)?([-#+ 0,(]+)?(\\d+)?([.]\\d+)?([bBhHsScCdoxXeEfgGaAtT])");
    
    static Object[] coolStuff(String pattern, Object[] args) {
        
        Class[] conversions = new Class[args.length];
        
        Matcher matcher = formatterPattern.matcher(pattern);
        int incrementalPosition = 1;
        while(matcher.find()) {
            String conversion = matcher.group(6);
            Integer position;
            if(matcher.group(2) == null) {
                position = incrementalPosition++;
            } else {
                position = Integer.parseInt(matcher.group(2));
            }
            if(conversion.equals("d") && position <= conversions.length) {
                conversions[position-1] = Long.class;
            }
            if(conversion.equals("f") && position <= conversions.length) {
                conversions[position-1] = Double.class;
            }
        }
        
        Object[] result = new Object[args.length];
        for(int i=0; i<args.length; i++) {
            if(args[i] == null) {
                continue;
            }
            if(conversions[i] == null) {
                result[i] = args[i];
            } else {
                try {
                    // TODO: I think we need to type of direct bind -> primitive and object binder
                    result[i] = OldBinder.directBind(null, args[i] + "", conversions[i]);
                } catch(Exception e) {
                    result[i] = null;
                }
            }
        }
        return result;
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
