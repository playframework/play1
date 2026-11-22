package play.data.validation;

import java.util.Map;

import play.exceptions.UnexpectedException;
import play.utils.Java;

public class ValidCheck extends AbstractAnnotationCheck<Required> {

    static final String mes = "validation.object";
    String key;

    @Override
    public boolean isSatisfied(Object validatedObject, Object value, PlayValidationContext context) {
        if (value == null) {
            return true;
        }
        Map<Object, String> keys = ValidationPlugin.keys.get();
        String superKey = keys != null ? keys.get(validatedObject) : null;
        try {
            if (context instanceof MethodParameterValidationContext ctx) {
                String[] paramNames = Java.parameterNames(ctx.getMethod());
                key = paramNames[ctx.getParameterIndex()];
            } else if (context instanceof FieldValidationContext ctx) {
                key = ctx.getField().getName();
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
        if (superKey != null) {
            key = superKey + "." + key;
        }
        return PlayValidator.validate(key, value, keys);
    }

}
