package play.data.validation;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeMap;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.FieldContext;
import net.sf.oval.context.OValContext;
import org.apache.commons.lang.StringUtils;
import play.db.jpa.GenericModel;
import play.db.jpa.JPQL;
import play.db.jpa.Model;
import play.exceptions.UnexpectedException;

/**
 * Check which proof if one or a set of properties is unique.
 *
 */
public class UniqueCheck extends AbstractAnnotationCheck<Unique> {

    final static String mes = "validation.unique";
    private String uniqueKeyContext = null;

    @Override
    public void configure(Unique constraintAnnotation) {
        uniqueKeyContext = constraintAnnotation.value();
        setMessage(constraintAnnotation.message());
    }

    @Override
    public Map<String, String> createMessageVariables() {
        Map<String, String> messageVariables = new TreeMap<String, String>();
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
        final String[] propertyNames = getPropertyNames(
                ((FieldContext) context).getField().getName());
        final GenericModel model = (GenericModel) validatedObject;
        final Model.Factory factory =  Model.Manager.factoryFor(model.getClass());
        final String keyProperty = factory.keyName();
        final Object keyValue = factory.keyValue(model);
        //In case of an update make sure that we won't read the current record from database.
        final boolean isUpdate = (keyValue != null);
        final String entityName = model.getClass().getName();
        final StringBuffer jpql = new StringBuffer("SELECT COUNT(o) FROM ");
        jpql.append(entityName).append(" AS o where ");
        final Object[] values = new Object[isUpdate ? propertyNames.length + 1 :
                propertyNames.length];
        final Class clazz = validatedObject.getClass();
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
            jpql.append("o.").append(propertyNames[i]).append(" = ?" + String.valueOf(index++) + " ");
        }
        if (isUpdate) {
            values[propertyNames.length] = keyValue;
            jpql.append(" and o.").append(keyProperty).append(" <>  ?" + String.valueOf(index++) + " ");
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
                    fieldName + " for an object of type " + clazz);
        }
        throw new UnexpectedException("Cannot get the field " +  fieldName +
                " for an object of type " + clazz);
    }
}