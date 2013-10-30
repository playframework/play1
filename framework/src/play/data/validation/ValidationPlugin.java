package play.data.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.oval.ConstraintViolation;
import net.sf.oval.context.MethodParameterContext;
import net.sf.oval.guard.Guard;
import play.PlayPlugin;
import play.exceptions.ActionNotFoundException;
import play.exceptions.UnexpectedException;
import play.utils.Java;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Http.Cookie;
import play.mvc.Scope;
import play.mvc.results.Result;

public class ValidationPlugin extends PlayPlugin {

    public static ThreadLocal<Map<Object, String>> keys = new ThreadLocal<Map<Object, String>>();

    private boolean isAwakingFromAwait() {
        Http.Request request = Http.Request.current();
        if (request == null) {
            return false;
        }

        // if CONTINUATIONS_STORE_VALIDATIONS is present we know that
        // we are awaking from await()
        return request.args.containsKey(ActionInvoker.CONTINUATIONS_STORE_VALIDATIONS);
    }

    @Override
    public void beforeInvocation() {
        keys.set(new HashMap<Object, String>());
        Validation.current.set(new Validation());
    }

    @Override
    public void beforeActionInvocation(Method actionMethod) {

        // when using await, this code get called multiple times.
        // When  recovering from await() we're going to restore (overwrite) validation.current
        // with the object-instance from the previous part of the execution.
        // If this is happening it is no point in doing anything here, since
        // we overwrite it later on.
        if (isAwakingFromAwait()) {
            return ;
        }

        try {
            Validation.current.set(restore());
            boolean verify = false;
            for (Annotation[] annotations : actionMethod.getParameterAnnotations()) {
                if (annotations.length > 0) {
                    verify = true;
                    break;
                }
            }
            if (!verify) {
                return;
            }
            List<ConstraintViolation> violations = new Validator().validateAction(actionMethod);
            ArrayList<Error> errors = new ArrayList<Error>();
            String[] paramNames = Java.parameterNames(actionMethod);
            for (ConstraintViolation violation : violations) {
                errors.add(new Error(paramNames[((MethodParameterContext) violation.getContext()).getParameterIndex()], violation.getMessage(), violation.getMessageVariables() == null ? new String[0] : violation.getMessageVariables().values().toArray(new String[0])));
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

    @Override
    public void onInvocationException(Throwable e) {
        clear();
    }

    @Override
    public void invocationFinally() {
        if (keys.get() != null) {
            keys.get().clear();
        }
        keys.remove();
        Validation.current.remove();
    }

    // ~~~~~~
    static class Validator extends Guard {

        public List<ConstraintViolation> validateAction(Method actionMethod) throws Exception {
            List<ConstraintViolation> violations = new ArrayList<ConstraintViolation>();
            Object instance = null;
            // Patch for scala defaults
            if (!Modifier.isStatic(actionMethod.getModifiers()) && actionMethod.getDeclaringClass().getSimpleName().endsWith("$")) {
                try {
                    instance = actionMethod.getDeclaringClass().getDeclaredField("MODULE$").get(null);
                } catch (Exception e) {
                    throw new ActionNotFoundException(Http.Request.current().action, e);
                }
            }
            Object[] rArgs = ActionInvoker.getActionMethodArgs(actionMethod, instance);
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
                    String[] g2 = matcher.group(2).split("\u0001");
                    String message = g2[0];
                    String[] args = new String[g2.length - 1];
                    System.arraycopy(g2, 1, args, 0, args.length);
                    validation.errors.add(new Error(matcher.group(1), message, args));
                }
            }
            return validation;
        } catch (Exception e) {
            return new Validation();
        }
    }

    static void save() {
        if (Http.Response.current() == null) {
            // Some request like WebSocket don't have any response
            return;
        }
        if (Validation.errors().isEmpty()) {
            // Only send "delete cookie" header when the cookie was present in the request
            if(Http.Request.current().cookies.containsKey(Scope.COOKIE_PREFIX + "_ERRORS") || !Scope.SESSION_SEND_ONLY_IF_CHANGED) {
                Http.Response.current().setCookie(Scope.COOKIE_PREFIX + "_ERRORS", "", "0s");
            }
            return;
        }
        try {
            StringBuilder errors = new StringBuilder();
            if (Validation.current() != null && Validation.current().keep) {
                for (Error error : Validation.errors()) {
                    errors.append("\u0000");
                    errors.append(error.key);
                    errors.append(":");
                    errors.append(error.message);
                    for (String variable : error.variables) {
                        errors.append("\u0001");
                        errors.append(variable);
                    }
                    errors.append("\u0000");
                }
            }
            String errorsData = URLEncoder.encode(errors.toString(), "utf-8");
            Http.Response.current().setCookie(Scope.COOKIE_PREFIX + "_ERRORS", errorsData);
        } catch (Exception e) {
            throw new UnexpectedException("Errors serializationProblem", e);
        }
    }

    static void clear() {
        try {
            if (Http.Response.current() != null && Http.Response.current().cookies != null) {
                Cookie cookie = new Cookie();
                cookie.name = Scope.COOKIE_PREFIX + "_ERRORS";
                cookie.value = "";
                cookie.sendOnError = true;
                Http.Response.current().cookies.put(cookie.name, cookie);
            }
        } catch (Exception e) {
            throw new UnexpectedException("Errors serializationProblem", e);
        }
    }
}
