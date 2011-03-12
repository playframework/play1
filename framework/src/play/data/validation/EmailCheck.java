package play.data.validation;

import java.util.regex.Pattern;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;

@SuppressWarnings("serial")
public class EmailCheck extends AbstractAnnotationCheck<Email> {

    final static String mes = "validation.email";
    static Pattern emailPattern = Pattern.compile("[\\w!#$%&'*+/=?^_`{|}~-]+(?:\\.[\\w!#$%&'*+/=?^_`{|}~-]+)*@(?:[\\w](?:[\\w-]*[\\w])?\\.)+[a-zA-Z0-9](?:[\\w-]*[\\w])?");

    @Override
    public void configure(Email email) {
        setMessage(email.message());
    }

    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        value = Validation.willBeValidated(value);
        if (value == null || value.toString().length() == 0) {
            return true;
        }
        return emailPattern.matcher(value.toString()).matches();
    }
   
}
