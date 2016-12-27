package play.data.validation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.FieldContext;
import net.sf.oval.context.MethodParameterContext;
import net.sf.oval.context.OValContext;
import play.data.binding.Binder;
import play.data.binding.RootParamNode;
import play.exceptions.UnexpectedException;
import play.utils.Java;
import play.mvc.Scope;

@SuppressWarnings("serial")
public class EqualsCheck extends AbstractAnnotationCheck<Equals> {

    static final String mes = "validation.equals";

    String to;
    String otherKey;
    Object otherValue;

    @Override
    public void configure(Equals equals) {
        this.to = equals.value();
        setMessage(equals.message());
    }

    @Override
    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        requireMessageVariablesRecreation();
        try {
            if (context != null) {
                if (context instanceof MethodParameterContext) {
                    MethodParameterContext ctx = (MethodParameterContext) context;
                    Method method = ctx.getMethod();
                    String[] paramNames = Java.parameterNames(method);
                    int index = -1;
                    for(int i=0; i<paramNames.length; i++) {
                        if(paramNames[i].equals(to)) {
                            index = i;
                            break;
                        }
                    }
                    if(index < 0) {
                        return false;
                    }
                    otherKey = to;

                    RootParamNode rootParamNode = Scope.Params.current().getRootParamNode();
                    Class<?> clazz = method.getParameterTypes()[index];
                    Type type = method.getGenericParameterTypes()[index];

                    otherValue = Binder.bind(rootParamNode, to, clazz, type, method.getParameterAnnotations()[index]);


                }
                if (context instanceof FieldContext) {
                    FieldContext ctx = (FieldContext) context;
                    try {
                        Field otherField = ctx.getField().getDeclaringClass().getDeclaredField(to);
                        otherKey = to;
                        otherValue = otherField.get(validatedObject);
                    } catch(Exception e) {
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
        if(value == null) {
            return otherValue == null;
        }
        return value.equals(otherValue);
    }

    @Override
    public Map<String, String> createMessageVariables() {
        Map<String, String> messageVariables = new HashMap<>();
        messageVariables.put("to", otherKey);
        return messageVariables;
    }
   
}
