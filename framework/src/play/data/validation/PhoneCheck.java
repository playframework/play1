package play.data.validation;

import java.util.regex.Pattern;

public class PhoneCheck extends AbstractAnnotationCheck<Phone> {

    static final String mes = "validation.phone";

    static final Pattern phonePattern = Pattern.compile("^([\\+][0-9]{1,3}([ \\.\\-]))?([\\(]{1}[0-9]{1,6}[\\)])?([0-9 \\.\\-/]{3,20})((x|ext|extension)[ ]?[0-9]{1,4})?$");

    @Override
    public void configure(Phone phone) {
        setMessage(phone.message());
    }

    @Override
    public boolean isSatisfied(Object validatedObject, Object value, PlayValidationContext context) {
        if (value == null || value.toString().length() == 0) {
            return true;
        }
        return phonePattern.matcher(value.toString()).matches();
    }

}
