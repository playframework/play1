package play.data.validation;

import java.util.HashMap;
import java.util.Map;

public class RangeCheck extends AbstractAnnotationCheck<Range> {

    static final String mes = "validation.range";

    double min;
    double max;

    @Override
    public void configure(Range range) {
        this.min = range.min();
        this.max = range.max();
        setMessage(range.message());
    }

    @Override
    public boolean isSatisfied(Object validatedObject, Object value, PlayValidationContext context) {
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
        Map<String, String> messageVariables = new HashMap<>();
        messageVariables.put("min", Double.toString(min));
        messageVariables.put("max", Double.toString(max));
        return messageVariables;
    }
   
}
