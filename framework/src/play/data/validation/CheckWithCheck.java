package play.data.validation;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.TreeMap;

import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;
import play.exceptions.UnexpectedException;

@SuppressWarnings("serial")
public class CheckWithCheck extends AbstractAnnotationCheck<CheckWith> {

    final static String mes = "validation.invalid";

    Map<String, String> variables = new TreeMap<String, String>();
    Check check;

    @Override
    public void configure(CheckWith checkWith) {
        setMessage(checkWith.message());
        try {
            Constructor<?> constructor = checkWith.value().getDeclaredConstructor();
            constructor.setAccessible(true);
            check = (Check)constructor.newInstance();
            check.checkWithCheck = this;
        } catch(Exception e) {
            throw new UnexpectedException(e);
        }
    }

    @Override
    protected Map<String, String> createMessageVariables() {
        return variables;
    }

    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        return check.isSatisfied(validatedObject, value);
    }

    public void setVariables() {
        requireMessageVariablesRecreation();
    }
}
