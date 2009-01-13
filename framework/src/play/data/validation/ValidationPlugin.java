package play.data.validation;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.oval.ConstraintViolation;
import net.sf.oval.context.MethodParameterContext;
import net.sf.oval.guard.Guard;
import play.PlayPlugin;
import play.exceptions.UnexpectedException;
import play.libs.Java;
import play.mvc.Http;
import play.mvc.Scope;
import play.mvc.results.Result;

public class ValidationPlugin extends PlayPlugin {

    @Override
    public void beforeInvocation() {
        Validation.current.set(restore());
    }

    @Override
    public void beforeActionInvocation(Method actionMethod) {
        try {
            List<ConstraintViolation> violations = new Validator().validateAction(actionMethod);
            ArrayList errors = new ArrayList();
            String[] paramNames = Java.parameterNames(actionMethod);
            for (ConstraintViolation violation : violations) {
                errors.add(new Error(paramNames[((MethodParameterContext) violation.getContext()).getParameterIndex()], violation.getMessage()));
            }
            Validation.current.get().errors.addAll(errors);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    @Override
    public void onActionInvocationResult(Result result) {
        save();
    }

    
    // ~~~~~~

    static class Validator extends Guard {

        public List<ConstraintViolation> validateAction(Method actionMethod) throws Exception {
            List<ConstraintViolation> violations = new ArrayList<ConstraintViolation>();
            Object[] rArgs = Java.prepareArgs(actionMethod, Scope.Params.current().all());
            validateMethodParameters(null, actionMethod, rArgs, violations);
            validateMethodPre(null, actionMethod, rArgs, violations);
            return violations;
        }
    }
    
    static Pattern errorsParser = Pattern.compile("\u0000([^:]*):([^\u0000]*)\u0000");

    static Validation restore() {
        try {
            Validation validation = new Validation();
            Http.Cookie cookie = Http.Request.current().cookies.get(Scope.COOKIE_PREFIX + "_ERRORS");
            if (cookie != null) {
                String errorsData = URLDecoder.decode(cookie.value, "utf-8");
                Matcher matcher = errorsParser.matcher(errorsData);
                while (matcher.find()) {
                    validation.errors.add(new Error(matcher.group(1), matcher.group(2)));
                }
            }
            return validation;
        } catch (Exception e) {
            throw new UnexpectedException("Errors corrupted", e);
        }
    }

    static void save() {
        try {
            StringBuilder errors = new StringBuilder();
            if(Validation.current().keep) {
                for (Error error : Validation.errors()) {
                    errors.append("\u0000");
                    errors.append(error.key);
                    errors.append(":");
                    errors.append(error.message);
                    errors.append("\u0000");
                }
            }
            String errorsData = URLEncoder.encode(errors.toString(), "utf-8");
            Http.Response.current().setCookie(Scope.COOKIE_PREFIX + "_ERRORS", errorsData);
        } catch (Exception e) {
            throw new UnexpectedException("Errors serializationProblem", e);
        }
    } 
}
