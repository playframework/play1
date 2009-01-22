package play.data.validation;

import java.util.Collection;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;
import play.exceptions.UnexpectedException;

public class RequiredCheck extends AbstractAnnotationCheck<Required> {
    
    final static String mes = "validation.required";

    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        if (value == null) {
            return false;
        }
        if (value instanceof String) {
            return value.toString().trim().length() > 0;
        }
        if (value instanceof Collection) {
            return ((Collection)value).size() > 0;
        }
        if (value.getClass().isArray()) {
            try {
            return value.getClass().getField("length").getInt(value) > 0;
            } catch(Exception e) {
                throw new UnexpectedException(e);
            }
        }
        return true;
    }
}
