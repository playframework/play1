package play.data.validation;

import java.util.HashMap;
import java.util.Map;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;

@SuppressWarnings("serial")
public class MinCheck extends AbstractAnnotationCheck<Min> {

    static final String mes = "validation.min";

    double min;

    @Override
    public void configure(Min min) {
        this.min = min.value();
        setMessage(min.message());
    }

    @Override
    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        requireMessageVariablesRecreation();
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(value.toString()) >= min;
            } catch (Exception e) {
                return false;
            }
        }
        if (value instanceof Number) {
            try {
                return ((Number) value).doubleValue() >= min;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public Map<String, String> createMessageVariables() {
        Map<String, String> messageVariables = new HashMap<>();
        messageVariables.put("min", Double.toString(min));
        return messageVariables;
    }
   
}
