package play.data.validation;

import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;

@SuppressWarnings("serial")
public class IsTrueCheck extends AbstractAnnotationCheck<IsTrue> {

    final static String mes = "validation.isTrue";

    @Override
    public void configure(IsTrue isTrue) {
        setMessage(isTrue.message());
    }

    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        if (value == null) {
            return false;
        }
        if (value instanceof String) {
            try {
                return Boolean.parseBoolean(value.toString());
            } catch (Exception e) {
                return false;
            }
        }
        if (value instanceof Number) {
            try {
                return ((Number) value).doubleValue() != 0;
            } catch (Exception e) {
                return false;
            }
        }
        if (value instanceof Boolean) {
            try {
                return ((Boolean) value);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
   
}
