package play.data.validation;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;

@SuppressWarnings("serial")
public class MatchCheck extends AbstractAnnotationCheck<Match> {

    static final String mes = "validation.match";
    Pattern pattern = null;

    @Override
    public void configure(Match match) {
        setMessage(match.message());
        pattern = Pattern.compile(match.value());
    }

    @Override
    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        requireMessageVariablesRecreation();
        if (value == null || value.toString().length() == 0) {
            return true;
        }
        return pattern.matcher(value.toString()).matches();
    }

    @Override
    public Map<String, String> createMessageVariables() {
        Map<String, String> messageVariables = new HashMap<>();
        messageVariables.put("pattern", pattern.toString());
        return messageVariables;
    }

}
