package play.data.validation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;
import play.utils.Utils.AlternativeDateFormat;
import play.exceptions.UnexpectedException;

@SuppressWarnings("serial")
public class InFutureCheck extends AbstractAnnotationCheck<InFuture> {

    final static String mes = "validation.future";

    Date reference;

    @Override
    public void configure(InFuture future) {
        try {
            this.reference = future.value().equals("") ? new Date() : AlternativeDateFormat.getDefaultFormatter().parse(future.value());
        } catch (ParseException ex) {
            throw new UnexpectedException("Cannot parse date " +future.value(), ex);
        }
        if(!future.value().equals("") && future.message().equals(mes)) {
            setMessage("validation.after");
        } else {
            setMessage(future.message());
        }
    }

    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        requireMessageVariablesRecreation();
        if (value == null) {
            return true;
        }
        if (value instanceof Date) {
            try {
                return reference.before((Date)value);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public Map<String, String> createMessageVariables() {
        Map<String, String> messageVariables = new HashMap<String, String>();
        messageVariables.put("reference", new SimpleDateFormat("yyyy-MM-dd").format(reference));
        return messageVariables;
    }
   
}
