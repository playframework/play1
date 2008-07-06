package play.i18n;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Messages {
    
    static protected Properties defaults;
    static protected Map<String,Properties> locales = new HashMap<String,Properties>();    
    
    public static String get(String key, Object... args) {
        String value = null;
        if(locales.containsKey(Lang.get())) {
            value = locales.get(Lang.get()).getProperty(key);
        }
        if(value == null) {
            value = defaults.getProperty(key);
        }
        if(value == null) {
            value = "!" + key + "!";
        }
        return String.format(value, args);
    }

}
