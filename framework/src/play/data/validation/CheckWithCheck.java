package play.data.validation;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.TreeMap;

import play.exceptions.UnexpectedException;

public class CheckWithCheck extends AbstractAnnotationCheck<CheckWith> {

    static final String mes = "validation.invalid";

    final Map<String, String> variables = new TreeMap<>();
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

    @Override
    public boolean isSatisfied(Object validatedObject, Object value, PlayValidationContext context) {
        return check.isSatisfied(validatedObject, value);
    }

    public void setVariables() {
        requireMessageVariablesRecreation();
    }
}
