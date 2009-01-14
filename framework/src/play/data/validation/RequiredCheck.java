package play.data.validation;

import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;

public class RequiredCheck extends AbstractAnnotationCheck<Required> {
    
    final static String mes = "validation.required";

    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        if (value == null) {
            return false;
        }
        if (value instanceof String) {
            return value.toString().trim().length() > 0;
        }
        return true;
    }
}
