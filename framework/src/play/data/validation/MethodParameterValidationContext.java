package play.data.validation;

import java.lang.reflect.Method;

/**
 * {@link PlayValidationContext} describing a parameter of the validated method.
 */
public final class MethodParameterValidationContext implements PlayValidationContext {

    private final Method method;
    private final int parameterIndex;

    MethodParameterValidationContext(Method method, int parameterIndex) {
        this.method = method;
        this.parameterIndex = parameterIndex;
    }

    public Method getMethod() {
        return method;
    }

    public int getParameterIndex() {
        return parameterIndex;
    }
}
