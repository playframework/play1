package play.data.validation;

import java.util.regex.Pattern;

import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;
import net.sf.oval.exception.OValException;

public class NoSpecialCharsCheck extends AbstractAnnotationCheck<NoSpecialChars> {

	final static String mes = "validation.nospecialchars";

	static private Pattern pattern = Pattern.compile("^[0-9a-zA-Z]+$");

	@Override
	public void configure(NoSpecialChars phone) {
		setMessage(phone.message());
	}

	@Override
	public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator)
			throws OValException {
		if (value == null || value.toString().length() == 0) {
			return true;
		}
		return pattern.matcher(value.toString()).matches();
	}

}