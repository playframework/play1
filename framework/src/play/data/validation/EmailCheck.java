package play.data.validation;

import java.util.regex.Pattern;

public class EmailCheck extends AbstractAnnotationCheck<Email> {

    static final String mes = "validation.email";
    static final Pattern emailPattern = Pattern.compile("[\\w!#$%&'*+/=?^_`{|}~-]+(?:\\.[\\w!#$%&'*+/=?^_`{|}~-]+)*@(?:[\\w](?:[\\w-]*[\\w])?\\.)+[a-zA-Z0-9](?:[\\w-]*[\\w])?");

    @Override
    public void configure(Email email) {
        setMessage(email.message());
    }

    @Override
    public boolean isSatisfied(Object validatedObject, Object value, PlayValidationContext context) {
        value = Validation.willBeValidated(value);
        if (value == null || value.toString().length() == 0) {
            return true;
        }
        return emailPattern.matcher(value.toString()).matches();
    }

}
