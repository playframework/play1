package play.data.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Validation {
    
    static ThreadLocal<Validation> current = new ThreadLocal<Validation>();
    
    //
    public List<Error> errors = new ArrayList();
    public boolean keep = false;
    
    public static Validation current() {
        return current.get();
    }
    
    public static List<Error> errors() {
        return current.get().errors;
    }
    
    public static Map<String, Error> errorsMap() {
        Map<String, Error> result = new HashMap<String, Error>();
        for(Error error : errors()) {
            result.put(error.key, error);
        }
        return result;
    }
    
    public static boolean validate(Object o) {
        return false;
    }
    
    public static boolean hasErrors() {
        return current.get().errors.size() > 0;
    }
    
    public static Error error(String key) {
        for(Error error : current.get().errors) {
            if(error.key.equals(key)) {
                return error;
            }
        }
        return null;
    }
    
    public static void keep() {
        current.get().keep = true;
    }
    
    public static boolean hasError(String key) {
        return error(key) != null;
    }

}
