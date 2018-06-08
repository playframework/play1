package play.data.validation;

import java.util.HashMap;
import java.util.Map;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;

@SuppressWarnings("serial")
public class MaxCheck extends AbstractAnnotationCheck<Max> {

    static final String mes = "validation.max";

    double max;

    @Override
    public void configure(Max max) {
        this.max = max.value();
        setMessage(max.message());
    }

    @Override
    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        requireMessageVariablesRecreation();
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(value.toString()) <= max;
            } catch (Exception e) {
                return false;
            }
        }
        if (value instanceof Number) {
            try {
                return ((Number) value).doubleValue() <= max;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public Map<String, String> createMessageVariables() {
        Map<String, String> messageVariables = new HashMap<>();
        messageVariables.put("max", Double.toString(max));
        return messageVariables;
    }
   
}
