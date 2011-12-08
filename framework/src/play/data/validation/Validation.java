package play.data.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import play.Play;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.exceptions.UnexpectedException;

public class Validation {

    public static ThreadLocal<Validation> current = new ThreadLocal<Validation>();
    List<Error> errors = new ArrayList<Error>();
    boolean keep = false;

    protected Validation() {
    }

    /**
     * @return The current validation helper
     */
    public static Validation current() {
        return current.get();
    }

    /**
     * @return The list of all errors
     */
    @SuppressWarnings({"serial", "unused"})
    public static List<Error> errors() {
        Validation validation = current.get();
        if (validation == null)
            return Collections.emptyList();
        
        return new ArrayList<Error>(validation.errors) {

            public Error forKey(String key) {
                return Validation.error(key);
            }

            public List<Error> allForKey(String key) {
                return Validation.errors(key);
            }
        };
    }

    /**
     * @return All errors keyed by field name
     */
    public Map<String, List<Error>> errorsMap() {
        Map<String, List<Error>> result = new LinkedHashMap<String, List<Error>>();
        for (Error error : errors()) {
            result.put(error.key, errors(error.key));
        }
        return result;
    }

    /**
     * Add an error
     * @param field Field name
     * @param message Message key
     * @param variables Message variables
     */
    public static void addError(String field, String message, String... variables) {
        if (error(field) == null || !error(field).message.equals(message)) {
            Validation.current().errors.add(new Error(field, message, variables));
        }
    }

    /**
     * @return True if the current request has errors
     */
    public static boolean hasErrors() {
        Validation validation = current.get();
        return validation != null && validation.errors.size() > 0;
    }

    /**
     * @param field The field name
     * @return First error related to this field
     */
    public static Error error(String field) {
        Validation validation = current.get();
        if (validation == null)
            return null;
          
        for (Error error : validation.errors) {
            if (error.key!=null && error.key.equals(field)) {
                return error;
            }
        }
        return null;
    }

    /**
     * @param field The field name
     * @return All errors related to this field
     */
    public static List<Error> errors(String field) {
        Validation validation = current.get();
        if (validation == null)
            return Collections.emptyList();
      
        List<Error> errors = new ArrayList<Error>();
        for (Error error : validation.errors) {
            if (error.key!=null && error.key.equals(field)) {
                errors.add(error);
            }
        }
        return errors;
    }

    /**
     * Keep errors for the next request (will be stored in a cookie)
     */
    public static void keep() {
        current.get().keep = true;
    }

    /**
     * @param field The field name
     * @return True is there are errors related to this field
     */
    public static boolean hasError(String field) {
        return error(field) != null;
    }

    public static void clear() {
        current.get().errors.clear();
        ValidationPlugin.keys.get().clear();
    }

    // ~~~~ Integration helper
    public static Map<String, List<Validator>> getValidators(Class<?> clazz, String name) {
        Map<String, List<Validator>> result = new HashMap<String, List<Validator>>();
        searchValidator(clazz, name, result);
        return result;
    }

    public static List<Validator> getValidators(Class<?> clazz, String property, String name) {
        try {
            List<Validator> validators = new ArrayList<Validator>();
            while (!clazz.equals(Object.class)) {
                try {
                    Field field = clazz.getDeclaredField(property);
                    for (Annotation annotation : field.getDeclaredAnnotations()) {
                        if (annotation.annotationType().getName().startsWith("play.data.validation")) {
                            Validator validator = new Validator(annotation);
                            validators.add(validator);
                            if (annotation.annotationType().equals(Equals.class)) {
                                validator.params.put("equalsTo", name + "." + ((Equals) annotation).value());
                            }
                            if (annotation.annotationType().equals(InFuture.class)) {
                                validator.params.put("reference", ((InFuture) annotation).value());
                            }
                            if (annotation.annotationType().equals(InPast.class)) {
                                validator.params.put("reference", ((InPast) annotation).value());
                            }
                        }
                    }
                    break;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            return validators;
        } catch (Exception e) {
            return new ArrayList<Validator>();
        }
    }

    static void searchValidator(Class<?> clazz, String name, Map<String, List<Validator>> result) {
        for (Field field : clazz.getDeclaredFields()) {

            List<Validator> validators = new ArrayList<Validator>();
            String key = name + "." + field.getName();
            boolean containsAtValid = false;
            for (Annotation annotation : field.getDeclaredAnnotations()) {
                if (annotation.annotationType().getName().startsWith("play.data.validation")) {
                    Validator validator = new Validator(annotation);
                    validators.add(validator);
                    if (annotation.annotationType().equals(Equals.class)) {
                        validator.params.put("equalsTo", name + "." + ((Equals) annotation).value());
                    }
                    if (annotation.annotationType().equals(InFuture.class)) {
                        validator.params.put("reference", ((InFuture) annotation).value());
                    }
                    if (annotation.annotationType().equals(InPast.class)) {
                        validator.params.put("reference", ((InPast) annotation).value());
                    }

                }
                if (annotation.annotationType().equals(Valid.class)) {
                    containsAtValid = true;
                }
            }
            if (!validators.isEmpty()) {
                result.put(key, validators);
            }
            if (containsAtValid) {
                searchValidator(field.getType(), key, result);
            }
        }
    }

    public static class Validator {

        public Annotation annotation;
        public Map<String, Object> params = new HashMap<String, Object>();

        public Validator(Annotation annotation) {
            this.annotation = annotation;
        }
    }

    // ~~~~ Validations
    public static class ValidationResult {

        public boolean ok = false;
        public Error error;

        public ValidationResult message(String message) {
            if (error != null) {
                error.message = message;
            }
            return this;
        }

        public ValidationResult key(String key) {
            if (error != null) {
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
        String key = getLocalName(o);
        return Validation.required(key, o);
    }

    public static ValidationResult min(String key, Object o, double min) {
        MinCheck check = new MinCheck();
        check.min = min;
        return applyCheck(check, key, o);
    }

    public ValidationResult min(Object o, double min) {
        String key = getLocalName(o);
        return Validation.min(key, o, min);
    }

    public static ValidationResult max(String key, Object o, double max) {
        MaxCheck check = new MaxCheck();
        check.max = max;
        return applyCheck(check, key, o);
    }

    public ValidationResult max(Object o, double max) {
        String key = getLocalName(o);
        return Validation.max(key, o, max);
    }

    public static ValidationResult future(String key, Object o, Date reference) {
        InFutureCheck check = new InFutureCheck();
        check.reference = reference;
        return applyCheck(check, key, o);
    }

    public ValidationResult future(Object o, Date reference) {
        String key = getLocalName(o);
        return Validation.future(key, o, reference);
    }

    public static ValidationResult future(String key, Object o) {
        InFutureCheck check = new InFutureCheck();
        check.reference = new Date();
        return applyCheck(check, key, o);
    }

    public ValidationResult future(Object o) {
        String key = getLocalName(o);
        return Validation.future(key, o, new Date());
    }

    public static ValidationResult past(String key, Object o, Date reference) {
        InPastCheck check = new InPastCheck();
        check.reference = reference;
        return applyCheck(check, key, o);
    }

    public ValidationResult past(Object o, Date reference) {
        String key = getLocalName(o);
        return Validation.past(key, o, reference);
    }

    public static ValidationResult past(String key, Object o) {
        InPastCheck check = new InPastCheck();
        check.reference = new Date();
        return applyCheck(check, key, o);
    }

    public ValidationResult past(Object o) {
        String key = getLocalName(o);
        return Validation.past(key, o, new Date());
    }

    public static ValidationResult match(String key, Object o, String pattern) {
        MatchCheck check = new MatchCheck();
        check.pattern = Pattern.compile(pattern);
        return applyCheck(check, key, o);
    }

    public ValidationResult match(Object o, String pattern) {
        String key = getLocalName(o);
        return Validation.match(key, o, pattern);
    }

    public static ValidationResult email(String key, Object o) {
        EmailCheck check = new EmailCheck();
        return applyCheck(check, key, o);
    }

    public ValidationResult email(Object o) {
        String key = getLocalName(o);
        return Validation.email(key, o);
    }

    public static ValidationResult url(String key, Object o) {
        URLCheck check = new URLCheck();
        return applyCheck(check, key, o);
    }

    public ValidationResult url(Object o) {
        String key = getLocalName(o);
        return Validation.url(key, o);
    }

    public static ValidationResult phone(String key, Object o) {
        PhoneCheck check = new PhoneCheck();
        return applyCheck(check, key, o);
    }

    public ValidationResult phone(Object o) {
        String key = getLocalName(o);
        return Validation.phone(key, o);
    }

    public static ValidationResult ipv4Address(String key, Object o) {
        IPv4AddressCheck check = new IPv4AddressCheck();
        return applyCheck(check, key, o);
    }

    public ValidationResult ipv4Address(Object o) {
        String key = getLocalName(o);
        return Validation.ipv4Address(key, o);
    }

    public static ValidationResult ipv6Address(String key, Object o) {
        IPv6AddressCheck check = new IPv6AddressCheck();
        return applyCheck(check, key, o);
    }

    public ValidationResult ipv6Address(Object o) {
        String key = getLocalName(o);
        return Validation.ipv6Address(key, o);
    }

    public static ValidationResult isTrue(String key, Object o) {
        IsTrueCheck check = new IsTrueCheck();
        return applyCheck(check, key, o);
    }

    public ValidationResult isTrue(Object o) {
        String key = getLocalName(o);
        return Validation.isTrue(key, o);
    }

    public static ValidationResult equals(String key, Object o, String otherName, Object to) {
        EqualsCheck check = new EqualsCheck();
        check.otherKey = otherName;
        check.otherValue = to;
        return applyCheck(check, key, o);
    }

    public ValidationResult equals(Object o, Object to) {
        String key = getLocalName(o);
        String otherKey = getLocalName(to);
        return Validation.equals(key, o, otherKey, to);
    }

    public static ValidationResult range(String key, Object o, double min, double max) {
        RangeCheck check = new RangeCheck();
        check.min = min;
        check.max = max;
        return applyCheck(check, key, o);
    }

    public ValidationResult range(Object o, double min, double max) {
        String key = getLocalName(o);
        return Validation.range(key, o, min, max);
    }

    public static ValidationResult minSize(String key, Object o, int minSize) {
        MinSizeCheck check = new MinSizeCheck();
        check.minSize = minSize;
        return applyCheck(check, key, o);
    }

    public ValidationResult minSize(Object o, int minSize) {
        String key = getLocalName(o);
        return Validation.minSize(key, o, minSize);
    }

    public static ValidationResult maxSize(String key, Object o, int maxSize) {
        MaxSizeCheck check = new MaxSizeCheck();
        check.maxSize = maxSize;
        return applyCheck(check, key, o);
    }

    public ValidationResult maxSize(Object o, int maxSize) {
        String key = getLocalName(o);
        return Validation.maxSize(key, o, maxSize);
    }

    public static ValidationResult valid(String key, Object o) {
        ValidCheck check = new ValidCheck();
        check.key = key;
        return applyCheck(check, key, o);
    }

    public ValidationResult valid(Object o) {
        String key = getLocalName(o);
        return Validation.valid(key, o);
    }

    static ValidationResult applyCheck(AbstractAnnotationCheck<?> check, String key, Object o) {
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

    static String getLocalName(Object o) {
        List<String> names = LocalVariablesNamesTracer.getAllLocalVariableNames(o);
        if (names.size() > 0) {
            return names.get(0);
        }
        return "";
    }

    public static Object willBeValidated(Object value) {
        return Play.pluginCollection.willBeValidated(value);
    }
}
