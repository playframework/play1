package play.data.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.exceptions.UnexpectedException;

public class Validation {

    static ThreadLocal<Validation> current = new ThreadLocal<Validation>();
    List<Error> errors = new ArrayList();
    boolean keep = false;
    
    protected Validation() {
    }

    static Validation current() {
        return current.get();
    }

    public static List<Error> errors() {
        return current.get().errors;
    }

    public Map<String, List<Error>> errorsMap() {
        Map<String, List<Error>> result = new HashMap<String, List<Error>>();
        for (Error error : errors()) {
            result.put(error.key, errors(error.key));
        }
        return result;
    }

    public static void addError(String key, String message, String... variables) {
        Validation.current().errors.add(new Error(key, message, variables));
    }

    public static boolean hasErrors() {
        return current.get().errors.size() > 0;
    }

    public static Error error(String key) {
        for (Error error : current.get().errors) {
            if (error.key.equals(key)) {
                return error;
            }
        }
        return null;
    }

    public static List<Error> errors(String key) {
        List<Error> errors = new ArrayList<Error>();
        for (Error error : current.get().errors) {
            if (error.key.equals(key)) {
                errors.add(error);
            }
        }
        return errors;
    }

    public static void keep() {
        current.get().keep = true;
    }

    public static boolean hasError(String key) {
        return error(key) != null;
    }
    
    // ~~~~ Validations
    
    public static class ValidationResult {
        
        public boolean ok = false;
        public Error error;
        
        public ValidationResult message(String message) {
            if(error != null) {
                error.message = message;
            }
            return this;
        }
        
        public ValidationResult key(String key) {
            if(error != null) {
                error.key = key;
            }
            return this;
        }
        
    }
    
    public static ValidationResult required(String key, Object o) {
        RequiredCheck check = new RequiredCheck();
        return applyCheck(check, key, o);
    }

    public ValidationResult required(Object o) {
        String key = LocalVariablesNamesTracer.getAllLocalVariableNames(o).get(0);
        return Validation.required(key, o);
    }
    
    public static ValidationResult min(String key, Object o, double min) {
        MinCheck check = new MinCheck();
        check.min = min;
        return applyCheck(check, key, o);
    }

    public ValidationResult min(Object o, double min) {
        String key = LocalVariablesNamesTracer.getAllLocalVariableNames(o).get(0);
        return Validation.min(key, o, min);
    }

    public static ValidationResult valid(String key, Object o) {
        ValidCheck check = new ValidCheck();
        check.key = key;
        return applyCheck(check, key, o);
    }

    public ValidationResult valid(Object o) {
        String key = LocalVariablesNamesTracer.getAllLocalVariableNames(o).get(0);
        return Validation.valid(key, o);
    }

    static ValidationResult applyCheck(AbstractAnnotationCheck check, String key, Object o) {
        try {
            ValidationResult result = new ValidationResult();
            if (!check.isSatisfied(o, o, null, null)) {
                Error error = new Error(key, check.getClass().getDeclaredField("mes").get(null) + "", check.getMessageVariables() == null ? new String[0] : check.getMessageVariables().values().toArray(new String[0]));
                Validation.current().errors.add(error);
                result.error = error;
                result.ok = false;
            } else {
                result.ok = true;
            }
            return result;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
}
