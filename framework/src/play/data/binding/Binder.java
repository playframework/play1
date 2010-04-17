package play.data.binding;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import play.mvc.Http.Request;

public abstract class Binder {

    public final static Object MISSING = new Object();

    public final static String URLENCODED = "application/x-www-form-urlencoded";
    public final static String MULTIPART = "multipart/form-data";
    public final static String XML = "text/xml";
    public final static String JSON = "application/json";

    static Map<String, Binder> binders = new HashMap<String,Binder>();
    static {
        binders.put(URLENCODED, new play.data.binding.urlencoded.Binder());
        binders.put(MULTIPART, new play.data.binding.multipart.Binder());
        binders.put(JSON, new play.data.binding.json.Binder());
    }

    public static Binder getBinder(String contentType) {
        return binders.get(contentType);
    }

    public static Binder getBinder() {
        return getBinder(Request.current().contentType);
    }
    
    public static <T> T directBind(String value, Class<T> type, Annotation[] annotations) {
        try {
            return (T)DirectBinder.directBind(value, type, annotations);
        } catch(Exception e) {
            return null;
        }
    }

    public static <T> T directBind(String value, Class<T> type) {
        return directBind(value, type, new Annotation[0]);
    }

    public abstract <T> T bindFromRequest(Class<T> type, Type gtype, Annotation[] annotations);
    
    public <T> T bindFromRequest(Class<T> type, Annotation[] annotations) {
        return bindFromRequest(type, null, annotations);
    }

    public <T> T bindFromRequest(Class<T> type) {
        return bindFromRequest(type, null, new Annotation[0]);
    }

}
