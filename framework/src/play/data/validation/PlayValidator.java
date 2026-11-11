package play.data.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import play.exceptions.UnexpectedException;
import play.utils.Java;

/**
 * The validation engine, built on top of Hibernate Validator
 * (the Jakarta Validation reference implementation).
 *
 * <p>Play annotations ({@code play.data.validation.*}) are validated here, field by
 * field and parameter by parameter, with the full context ({@link PlayValidationContext}) they
 * need. Standard {@code jakarta.validation} constraints found on the validated beans
 * are delegated to the Jakarta Validation {@link jakarta.validation.Validator}.</p>
 */
public final class PlayValidator {

    /** static mes key of {@link ValidCheck}. */
    static final String OBJECT_ERROR = "validation.object";

    private static final String[] EMPTY_STRING_ARRAY = {};

    private static final Map<Class<? extends Annotation>, Class<? extends AbstractAnnotationCheck<?>>> CHECKS = Map.ofEntries(
        entry(Required.class, RequiredCheck.class),
        entry(Min.class, MinCheck.class),
        entry(Max.class, MaxCheck.class),
        entry(Range.class, RangeCheck.class),
        entry(MinSize.class, MinSizeCheck.class),
        entry(MaxSize.class, MaxSizeCheck.class),
        entry(Email.class, EmailCheck.class),
        entry(URL.class, URLCheck.class),
        entry(Phone.class, PhoneCheck.class),
        entry(IPv4Address.class, IPv4AddressCheck.class),
        entry(IPv6Address.class, IPv6AddressCheck.class),
        entry(IsTrue.class, IsTrueCheck.class),
        entry(Match.class, MatchCheck.class),
        entry(InFuture.class, InFutureCheck.class),
        entry(InPast.class, InPastCheck.class),
        entry(Equals.class, EqualsCheck.class),
        entry(Unique.class, UniqueCheck.class),
        entry(CheckWith.class, CheckWithCheck.class),
        entry(Valid.class, ValidCheck.class)
    );

    private static volatile ValidatorFactory factory;

    private PlayValidator() {
    }

    /**
     * Creates a check instance for a play validation annotation.
     *
     * @return null when the annotation is not a check annotation
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static AbstractAnnotationCheck<?> createCheck(Annotation annotation) {
        Class<? extends AbstractAnnotationCheck<?>> checkClass = CHECKS.get(annotation.annotationType());
        if (checkClass == null) {
            return null;
        }
        AbstractAnnotationCheck check;
        try {
            check = checkClass.getDeclaredConstructor().newInstance();
            check.setMessage(messageOf(annotation));
            check.configure(annotation);
        } catch (Exception e) {
            throw new UnexpectedException("Cannot instantiate check " + checkClass.getName() + " for " + annotation, e);
        }
        return check;
    }

    private static String messageOf(Annotation annotation) {
        try {
            return (String) annotation.annotationType().getMethod("message").invoke(annotation);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validates an object graph: play checks on each field, cascading into
     * {@link Valid}-annotated fields, and standard Jakarta Validation constraints
     * via Hibernate Validator.
     *
     * @param key key prefix for the errors of this object
     * @param value the object to validate
     * @param keys object-to-key map used to resolve nested keys (see {@link ValidationPlugin#keys})
     * @return true when the object is valid
     */
    public static boolean validate(String key, Object value, Map<Object, String> keys) {
        return validate(key, value, keys, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    /**
     * Validates the parameters of an action method: play checks and Jakarta
     * Validation parameter constraints.
     *
     * @param actionMethod the action method
     * @param args the bound arguments
     * @param instance the controller instance (null for static actions)
     */
    public static void validateParameters(Method actionMethod, Object[] args, Object instance) {
        Map<Object, String> keys = currentKeys();
        String[] paramNames = parameterNames(actionMethod);

        Annotation[][] parameterAnnotations = actionMethod.getParameterAnnotations();
        boolean hasJakartaConstraints = false;
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if (annotationType.getName().startsWith("play.data.validation")) {
                    if (annotationType.equals(Valid.class)) {
                        if (args[i] != null) {
                            if (!validate(paramNames[i], args[i], keys)) {
                                // replicate the legacy ValidCheck behaviour: the check
                                // itself failed, report the object level error
                                addObjectError(paramNames[i]);
                            }
                        }
                        continue;
                    }
                    AbstractAnnotationCheck<?> check = createCheck(annotation);
                    if (check == null) {
                        continue;
                    }
                    if (!check.isSatisfied(null, args[i], PlayValidationContext.parameter(actionMethod, i))) {
                        addError(paramNames[i], check);
                    }
                } else if (annotationType.isAnnotationPresent(jakarta.validation.Constraint.class)) {
                    hasJakartaConstraints = true;
                }
            }
        }

        if (hasJakartaConstraints) {
            Set<ConstraintViolation<Object>> violations = jakartaValidator()
                .forExecutables()
                .validateParameters(instance, actionMethod, args);
            for (ConstraintViolation<Object> violation : violations) {
                play.data.validation.Validation.current().errors.add(new Error(parameterKey(violation.getPropertyPath(), paramNames), violation.getMessage(), EMPTY_STRING_ARRAY));
            }
        }
    }

    private static boolean validate(String key, Object value, Map<Object, String> keys, Set<Object> visiting) {
        if (value == null) {
            return true;
        }
        if (value instanceof Collection<?> collection) {
            boolean everythingIsValid = true;
            int index = 0;
            for (Object item : collection) {
                String itemKey = key + "[" + index + "]";
                if (!validate(itemKey, item, keys, visiting)) {
                    addObjectError(itemKey);
                    everythingIsValid = false;
                }
                index++;
            }
            return everythingIsValid;
        }
        return validateSingle(key, value, keys, visiting);
    }

    private static boolean validateSingle(String key, Object value, Map<Object, String> keys, Set<Object> visiting) {
        // cycle: this object is already being validated higher up the graph
        if (value == null || !visiting.add(value)) {
            return true;
        }
        try {
            if (keys != null) {
                keys.put(value, key);
            }
            return validateFields(key, value, keys, visiting) && validateJakarta(key, value);
        } finally {
            visiting.remove(value);
        }
    }

    private static boolean validateFields(String key, Object value, Map<Object, String> keys, Set<Object> visiting) {
        boolean ok = true;
        for (Class<?> c = value.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Object fieldValue = getFieldValue(value, field);
                for (Annotation annotation : field.getDeclaredAnnotations()) {
                    Class<? extends Annotation> annotationType = annotation.annotationType();
                    if (!annotationType.getName().startsWith("play.data.validation")) {
                        // standard jakarta constraints and jakarta @Valid are
                        // handled by Hibernate Validator (see validateJakarta)
                        continue;
                    }
                    if (annotationType.equals(Valid.class)) {
                        if (fieldValue != null && !visiting.contains(fieldValue)) {
                            String nestedKey = key + "." + field.getName();
                            if (!validate(nestedKey, fieldValue, keys, visiting)) {
                                // replicate the legacy ValidCheck behaviour: the check
                                // itself failed, report the object level error
                                addObjectError(nestedKey);
                                ok = false;
                            }
                        }
                        continue;
                    }
                    AbstractAnnotationCheck<?> check = createCheck(annotation);
                    if (check == null) {
                        continue;
                    }
                    if (!check.isSatisfied(value, fieldValue, PlayValidationContext.field(field))) {
                        addError(key + "." + field.getName(), check);
                        ok = false;
                    }
                }
            }
        }
        return ok;
    }

    /**
     * Validates standard {@code jakarta.validation} constraints of the object with
     * Hibernate Validator and reports the violations as play errors.
     */
    private static boolean validateJakarta(String key, Object value) {
        Set<ConstraintViolation<Object>> violations = jakartaValidator().validate(value);
        if (violations.isEmpty()) {
            return true;
        }
        for (ConstraintViolation<Object> violation : violations) {
            String propertyKey = (key == null || key.isEmpty()) ? "" : key + ".";
            play.data.validation.Validation.current().errors.add(new Error(propertyKey + violation.getPropertyPath(), violation.getMessage(), EMPTY_STRING_ARRAY));
        }
        return false;
    }

    private static void addObjectError(String key) {
        play.data.validation.Validation.current().errors.add(new Error(key, OBJECT_ERROR, EMPTY_STRING_ARRAY));
    }

    private static void addError(String key, AbstractAnnotationCheck<?> check) {
        Map<String, String> variables = check.getMessageVariables();
        play.data.validation.Validation.current().errors.add(
            new Error(
                key,
                check.getMessageKey(),
                variables == null ? EMPTY_STRING_ARRAY : variables.values().toArray(String[]::new),
                check.getSeverity()
            )
        );
    }

    private static Object getFieldValue(Object value, Field field) {
        try {
            field.setAccessible(true);
            return field.get(value);
        } catch (Exception e) {
            throw new UnexpectedException("Cannot read field " + field + " of " + value.getClass(), e);
        }
    }

    /**
     * @return the Jakarta Validation validator (Hibernate Validator)
     */
    static jakarta.validation.Validator jakartaValidator() {
        return validatorFactory().getValidator();
    }

    /**
     * @return the shared Jakarta Validation validator factory, built with
     * {@link ParameterMessageInterpolator}. The default message interpolator needs
     * a Unified EL implementation ({@code jakarta.el}) on the classpath, which play
     * does not ship; {@code ParameterMessageInterpolator} only interpolates
     * {@code {parameter}} terms, without EL, and play resolves its message keys
     * ({@code validation.*}) through its own {@code Messages}, not through EL anyway.
     */
    public static ValidatorFactory validatorFactory() {
        if (factory == null) {
            synchronized (PlayValidator.class) {
                if (factory == null) {
                    factory = Validation.byDefaultProvider().configure()
                        .messageInterpolator(new ParameterMessageInterpolator())
                        .buildValidatorFactory();
                }
            }
        }
        return factory;
    }

    private static Map<Object, String> currentKeys() {
        Map<Object, String> keys = ValidationPlugin.keys.get();
        return keys != null ? keys : new HashMap<>();
    }

    private static String[] parameterNames(Method actionMethod) {
        try {
            return Java.parameterNames(actionMethod);
        } catch (Exception e) {
            throw new UnexpectedException("Cannot resolve parameter names of " + actionMethod, e);
        }
    }

    /**
     * Builds the play error key for a method validation violation: the parameter
     * name for the root node, followed by the property path within the parameter.
     */
    private static String parameterKey(Path path, String[] paramNames) {
        StringBuilder sb = new StringBuilder();
        for (Path.Node node : path) {
            if (node.getKind() == jakarta.validation.ElementKind.PARAMETER) {
                int index = node.getIndex() != null ? node.getIndex() : 0;
                sb.append(index < paramNames.length ? paramNames[index] : node.getName());
            } else {
                if (!sb.isEmpty()) {
                    sb.append('.');
                }
                sb.append(node.getName());
            }
        }
        return sb.toString();
    }
}
