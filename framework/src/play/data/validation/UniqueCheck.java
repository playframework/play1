package play.data.validation;

import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.FieldContext;
import net.sf.oval.context.OValContext;
import play.db.jpa.GenericModel;
import play.db.jpa.JPQL;
import play.db.jpa.Model;
import play.exceptions.UnexpectedException;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeMap;

/**
 * Check which proof if one or a set of properties is unique.
 *
 */
public class UniqueCheck extends AbstractAnnotationCheck<Unique> {

    static final String mes = "validation.unique";
    private String uniqueKeyContext = null;

    @Override
    public void configure(Unique constraintAnnotation) {
        uniqueKeyContext = constraintAnnotation.value();
        setMessage(constraintAnnotation.message());
    }

    @Override
    public Map<String, String> createMessageVariables() {
        Map<String, String> messageVariables = new TreeMap<>();
        messageVariables.put("2-properties", uniqueKeyContext);
        return messageVariables;
    }

    private String[] getPropertyNames(String uniqueKey) {
        String completeUniqueKey;
        if (uniqueKeyContext.length() > 0) {
            completeUniqueKey = uniqueKeyContext + ";" + uniqueKey;
        } else {
            completeUniqueKey = uniqueKey;
        }
        return completeUniqueKey.split("[,;\\s][\\s]*");
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isSatisfied(Object validatedObject, Object value,
            OValContext context, Validator validator) {
        requireMessageVariablesRecreation();
        if (value == null) {
            return true;
        }
        String[] propertyNames = getPropertyNames(
                ((FieldContext) context).getField().getName());
        GenericModel model = (GenericModel) validatedObject;
        Model.Factory factory =  Model.Manager.factoryFor(model.getClass());
        String keyProperty = factory.keyName();
        Object keyValue = factory.keyValue(model);
        //In case of an update make sure that we won't read the current record from database.
        boolean isUpdate = (keyValue != null);
        String entityName = model.getClass().getName();
        StringBuilder jpql = new StringBuilder("SELECT COUNT(o) FROM ");
        jpql.append(entityName).append(" AS o where ");
        Object[] values = new Object[isUpdate ? propertyNames.length + 1 :
                propertyNames.length];
        Class clazz = validatedObject.getClass();
        int index = 1;
        for (int i = 0; i < propertyNames.length; i++) {
            Field field = getField(clazz, propertyNames[i]);
            field.setAccessible(true);
            try {
                values[i] = field.get(model);
            } catch (Exception ex) {
                throw new UnexpectedException(ex);
            }
            if (i > 0) {
                jpql.append(" And ");
            }
            jpql.append("o.").append(propertyNames[i]).append(" = ?").append(String.valueOf(index++)).append(" ");
        }
        if (isUpdate) {
            values[propertyNames.length] = keyValue;
            jpql.append(" and o.").append(keyProperty).append(" <>  ?").append(String.valueOf(index++)).append(" ");
        }
        return JPQL.instance.count(entityName, jpql.toString(), values) == 0L;
    }

    private Field getField(Class clazz, String fieldName) {
        Class c = clazz;
        try {
            while (!c.equals(Object.class)) {
                try {
                    return c.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    c = c.getSuperclass();
                }
            }
        } catch (Exception e) {
            throw new UnexpectedException("Error while determining the field " +
                    fieldName + " for an object of type " + clazz, e);
        }
        throw new UnexpectedException("Cannot get the field " +  fieldName +
                " for an object of type " + clazz);
    }
}