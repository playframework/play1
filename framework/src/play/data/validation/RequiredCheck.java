package play.data.validation;

import java.util.Collection;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;
import play.db.Model.BinaryField;
import play.exceptions.UnexpectedException;

@SuppressWarnings("serial")
public class RequiredCheck extends AbstractAnnotationCheck<Required> {
    
    static final String mes = "validation.required";

    @Override
    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        if (value == null) {
            return false;
        }
        if (value instanceof String v) {
            return !v.isBlank();
        }
        if (value instanceof Collection<?> v) {
            return !v.isEmpty();
        }
        if (value instanceof BinaryField v) {
            return v.exists();
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
