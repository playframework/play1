package play.data.validation;

import java.util.List;
import net.sf.oval.ConstraintViolation;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.FieldContext;
import net.sf.oval.context.MethodParameterContext;
import net.sf.oval.context.OValContext;
import play.exceptions.UnexpectedException;
import play.utils.Java;

public class ValidCheck extends AbstractAnnotationCheck<Required> {

	private static final long serialVersionUID = 4245428250487460293L;
	final static String mes = "validation.object";
    String key;

    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        String superKey = ValidationPlugin.keys.get().get(validatedObject);
        if (value == null) {
            return true;
        }
        try {
            if (context != null) {
                if (context instanceof MethodParameterContext) {
                    MethodParameterContext ctx = (MethodParameterContext) context;
                    String[] paramNames = Java.parameterNames(ctx.getMethod());
                    key = paramNames[ctx.getParameterIndex()];
                }
                if (context instanceof FieldContext) {
                    FieldContext ctx = (FieldContext) context;
                    key = ctx.getField().getName();
                }
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
        if (superKey != null) {
            key = superKey + "." + key;
        }
        ValidationPlugin.keys.get().put(value, key);
        List<ConstraintViolation> violations = new Validator().validate(value);
        //
        if (violations.isEmpty()) {
            return true;
        } else {
            for (ConstraintViolation violation : violations) {
                if (violation.getContext() instanceof FieldContext) {
                    FieldContext ctx = (FieldContext) violation.getContext();
                    String fkey = key + "." + ctx.getField().getName();
                    Error error = new Error(fkey, violation.getMessage(), violation.getMessageVariables() == null ? new String[0] : violation.getMessageVariables().values().toArray(new String[0]));
                    Validation.current().errors.add(error);
                }
            }
            return false;
        }
    }
}
