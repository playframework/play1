package play.data.validation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Describes where a {@link Check} or {@link AbstractAnnotationCheck} is applied, mirroring
 * what the oVal context classes used to provide: a field of a validated bean, or a
 * parameter of a validated (action) method.
 *
 * <p>Checks may receive a {@code null} context when they are invoked programmatically
 * (eg. {@code Validation.required(...)}).</p>
 */
public sealed interface PlayValidationContext permits FieldValidationContext, MethodParameterValidationContext {

    /**
     * Creates a context for a field of a validated object.
     */
    static FieldValidationContext field(Field field) {
        return new FieldValidationContext(field);
    }

    /**
     * Creates a context for a parameter of a validated method.
     */
    static MethodParameterValidationContext parameter(Method method, int parameterIndex) {
        return new MethodParameterValidationContext(method, parameterIndex);
    }
}
