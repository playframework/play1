package play.data.validation;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;

@SuppressWarnings("serial")
public class InArrayCheck extends AbstractAnnotationCheck<Min> {

    final static String mes = "validation.array";

    Object[] array;

    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        requireMessageVariablesRecreation();
        
        if (value != null) {
            for (Object element : this.array) {
                if ((element != null) && element.toString().equals(value.toString())) {
                    return true;
                }
            }
        } else {
            return true;
        }
        
        return false;
    }

    @Override
    public Map<String, String> createMessageVariables() {
        Map<String, String> messageVariables = new HashMap<String, String>();
        
        messageVariables.put("array", StringUtils.join(this.array, ", "));
        
        return messageVariables;
    }
}