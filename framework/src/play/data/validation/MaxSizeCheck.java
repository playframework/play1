package play.data.validation;

import java.util.HashMap;
import java.util.Map;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;

@SuppressWarnings("serial")
public class MaxSizeCheck extends AbstractAnnotationCheck<MaxSize> {

    static final String mes = "validation.maxSize";

    int maxSize;

    @Override
    public void configure(MaxSize annotation) {
        this.maxSize = annotation.value();
        setMessage(annotation.message());
    }

    @Override
    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        requireMessageVariablesRecreation();
        if (value == null || value.toString().length() == 0) {
            return true;
        }
        return value.toString().length() <= maxSize;
    }

    @Override
    public Map<String, String> createMessageVariables() {
        Map<String, String> messageVariables = new HashMap<>();
        messageVariables.put("maxSize", Integer.toString(maxSize));
        return messageVariables;
    }
   
}
