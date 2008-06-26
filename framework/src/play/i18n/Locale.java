package play.i18n;

import play.Logger;
import play.Play;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class Locale {
    
    public static ThreadLocal<String> current = new ThreadLocal<String>();
    
    public static String get() {    
        return current.get();
    }
    
    public static boolean set(String locale) {
        if(locale.equals("") || Play.locales.contains(locale)) {
            current.set(locale);
            return true;
        } else {
            Logger.warn("Locale %s is not defined in your application.conf", locale);
            return false;
        }
    }
    
    public static void change(String locale) {
        if(set(locale)) {
           Response.current().setCookie("PLAY_LOCALE", locale);
        }
    }
    
    public static void resolvefrom(Request request) {
        // Check a cookie
        if(request.cookies.containsKey("PLAY_LOCALE")) {
            if(!set(request.cookies.get("PLAY_LOCALE").value)) {
                Response.current().setCookie("PLAY_LOCALE", "");
            }
            return;
        }
        // Try from accept-language
        if(request.headers.containsKey("accept-language")) {
            String al = request.headers.get("accept-language").value();
            for(String a : al.split(",")) {
                if(a.indexOf(";") > 0) {
                    a = a.substring(0, a.indexOf(";"));
                }
                if(a.indexOf("-") > 0) {
                    a = a.substring(0, a.indexOf("-"));
                }
                for(String locale : Play.locales) {
                    if(locale.equals(a)) {
                        set(locale);
                        return;
                    }
                    
                }
            }            
        }
        // Use default
        if(Play.locales.isEmpty()) {
            set("");
        } else {
            set(Play.locales.get(0));
        }
    }

}
