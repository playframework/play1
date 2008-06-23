package play.i18n;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import play.Logger;
import play.Play;
import play.libs.Files;
import play.vfs.VirtualFile;

public class Messages {
    
    static public Properties defaults;
    static public Map<String,Properties> locales = new HashMap<String,Properties>();
    static Long lastLoading = 0L;
    
    public static String get(String key, Object... args) {
        String value = null;
        if(locales.containsKey(Locale.get())) {
            value = locales.get(Locale.get()).getProperty(key);
        }
        if(value == null) {
            value = defaults.getProperty(key);
        }
        if(value == null) {
            value = "!" + key + "!";
        }
        return String.format(value, args);
    }
    
    public static void load() {
        defaults = read(Play.getFile("conf/messages"));
        if(defaults == null) {
            defaults = new Properties();
        }
        for(String locale : Play.locales) {
            Properties properties = read(Play.getFile("conf/messages."+locale));
            if(properties == null) {
                Logger.warn("conf/messages.%s is missing", locale);
                locales.put(locale, new Properties());
            } else {
                locales.put(locale, properties);
            }
        }
        lastLoading = System.currentTimeMillis();
    }
    
    static Properties read(VirtualFile vf) {
        try {
            if(vf.exists()) {
                return Files.readUtf8Properties(vf.inputstream());                
            }
            return null;
        } catch(IOException e) {
            Logger.error(e, "Error while loading messages %s", vf.getName());
            return null;
        }
    }
    
    public static void detectChanges() {
        if(Play.getFile("conf/messages").exists() && Play.getFile("conf/messages").lastModified() > lastLoading) {
            load();
            return;
        }
        for(String locale : Play.locales) {
            if(Play.getFile("conf/messages."+locale).exists() && Play.getFile("conf/messages."+locale).lastModified() > lastLoading) {
                load();
                return;
            }
        }
    }

}
