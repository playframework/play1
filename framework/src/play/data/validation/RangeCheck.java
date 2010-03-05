package play.data.validation;

import java.util.HashMap;
import java.util.Map;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;

@SuppressWarnings("serial")
public class RangeCheck extends AbstractAnnotationCheck<Range> {

    final static String mes = "validation.range";

    double min;
    double max;

    @Override
    public void configure(Range range) {
        this.min = range.min();
        this.max = range.max();
        setMessage(range.message());
    }

    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        requireMessageVariablesRecreation();
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            try {
                double v = Double.parseDouble(value.toString());
                return v >= min && v <= max;
            } catch (Exception e) {
                return false;
            }
        }
        if (value instanceof Number) {
            try {
                return ((Number) value).doubleValue() >= min && ((Number) value).doubleValue() <= max;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public Map<String, String> createMessageVariables() {
        Map<String, String> messageVariables = new HashMap<String, String>();
        messageVariables.put("min", Double.toString(min));
        messageVariables.put("max", Double.toString(max));
        return messageVariables;
    }
   
}
