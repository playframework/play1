package play.data.validation;

import java.util.regex.Pattern;

import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;
import net.sf.oval.exception.OValException;

public class PhoneCheck extends AbstractAnnotationCheck<Phone> {

	final static String mes = "validation.phone";

<<<<<<< HEAD
	static Pattern phonePattern = Pattern
	.compile("^([\\+][0-9]{1,3}([ \\.\\-]))?([\\(]{1}[0-9]{2,6}[\\)])?([0-9 \\.\\-/]{3,20})((x|ext|extension)[ ]?[0-9]{1,4})?$");
=======
	static private Pattern phonePattern = Pattern
			.compile("^([\\+][0-9]{1,3}([ \\.\\-])?)?([\\(]{1}[0-9]{3}[\\)])?([0-9A-Z \\.\\-]{1,32})((x|ext|extension)[ ]?[0-9]{1,4})?$");
>>>>>>> bf3829351cd50bea5e894840523857f6b309801f

	@Override
	public void configure(Phone phone) {
		setMessage(phone.message());
	}

	@Override
	public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator)
			throws OValException {
		if (value == null || value.toString().length() == 0) {
			return true;
		}
		return phonePattern.matcher(value.toString()).matches();
	}

}