package play.data.validation;

import java.util.regex.Pattern;

import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;
import net.sf.oval.exception.OValException;

public class OnlyLettersCheck extends AbstractAnnotationCheck<OnlyLetters> {

	final static String mes = "validation.onlyletters";

	static private Pattern pattern = Pattern.compile("^[a-zA-Z \\']+$");

	@Override
	public void configure(OnlyLetters phone) {
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