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
import play.libs.I18N;

@SuppressWarnings("serial")
public class InPastCheck extends AbstractAnnotationCheck<InPast> {

    static final String mes = "validation.past";
    Date reference;

    @Override
    public void configure(InPast past) {
        try {
            this.reference = past.value().equals("") ? new Date() : AlternativeDateFormat.getDefaultFormatter().parse(past.value());
        } catch (ParseException ex) {
            throw new UnexpectedException("Cannot parse date " + past.value(), ex);
        }
        if (!past.value().equals("") && past.message().equals(mes)) {
            setMessage("validation.before");
        } else {
            setMessage(past.message());
        }
    }

    @Override
    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        requireMessageVariablesRecreation();
        if (value == null) {
            return true;
        }
        if (value instanceof Date) {
            try {
                return reference.after((Date) value);
            } catch (Exception e) {
                return false;
            }
        }
        if (value instanceof Long) {
            try {
                return reference.after(new Date((Long) value));
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public Map<String, String> createMessageVariables() {
        Map<String, String> messageVariables = new HashMap<>();
        messageVariables.put("reference", new SimpleDateFormat(I18N.getDateFormat()).format(reference));
        return messageVariables;
    }
}
