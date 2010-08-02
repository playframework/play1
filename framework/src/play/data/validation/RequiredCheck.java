package play.data.validation;

import java.util.Collection;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;
import play.db.Model.BinaryField;
import play.exceptions.UnexpectedException;

@SuppressWarnings("serial")
public class RequiredCheck extends AbstractAnnotationCheck<Required> {
    
    final static String mes = "validation.required";

    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        if (value == null) {
            return false;
        }
        if (value instanceof String) {
            return value.toString().trim().length() > 0;
        }
        if (value instanceof Collection<?>) {
            return ((Collection<?>)value).size() > 0;
        }
        if (value instanceof BinaryField) {
            return ((BinaryField)value).exists();
        }
        if (value.getClass().isArray()) {
            try {
                return java.lang.reflect.Array.getLength(value) > 0;
            } catch(Exception e) {
                throw new UnexpectedException(e);
            }
        }
        return true;
    }
}
