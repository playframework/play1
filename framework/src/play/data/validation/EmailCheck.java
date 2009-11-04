package play.data.validation;

import java.util.regex.Pattern;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;

public class EmailCheck extends AbstractAnnotationCheck<Email> {

	private static final long serialVersionUID = -6406701235879702909L;
	final static String mes = "validation.email";
    static Pattern emailPattern = Pattern.compile("[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?");

    @Override
    public void configure(Email email) {
        setMessage(email.message());
    }

    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        if (value == null || value.toString().length() == 0) {
            return true;
        }
        return emailPattern.matcher(value.toString()).matches();
    }
   
}
