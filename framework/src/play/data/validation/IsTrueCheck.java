package play.data.validation;

import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;

@SuppressWarnings("serial")
public class IsTrueCheck extends AbstractAnnotationCheck<IsTrue> {

    static final String mes = "validation.isTrue";

    @Override
    public void configure(IsTrue isTrue) {
        setMessage(isTrue.message());
    }

    @Override
    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        if (value == null) {
            return false;
        }
        if (value instanceof String v) {
            try {
                return Boolean.parseBoolean(v);
            } catch (Exception e) {
                return false;
            }
        }
        if (value instanceof Number v) {
            try {
                return v.doubleValue() != 0;
            } catch (Exception e) {
                return false;
            }
        }
        if (value instanceof Boolean v) {
            try {
                return v;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
   
}
