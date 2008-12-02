package play.data.binding;

import java.lang.reflect.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import play.Logger;

/**
 * Parameters map to POJO binder
 */
public class BeanWrapper {

    final static int notwritableField = Modifier.FINAL | Modifier.NATIVE | Modifier.STATIC;

    final static int notaccessibleMethod = Modifier.NATIVE | Modifier.STATIC;

    private Class beanClass;

    /** 
     * a cache for our properties and setters
     */
    private Map<String, Property> wrappers = new HashMap();

    public BeanWrapper(Class forClass) {
        Logger.debug("new bean wrapper for class %s", forClass.getName());
        this.beanClass = forClass;
        registerSetters(forClass);
        registerFields(forClass);
    }

    public Collection<Property> getWrappers() {
        return wrappers.values();
    }

    public Object bind(String name, Type type, Map<String, String[]> params, String prefix) throws Exception {
        Object instance = newBeanInstance();
        for (Property prop : wrappers.values()) {
            String newPrefix = prefix + "." + prop.getName();
            prop.setValue(instance, Binder.bindInternal(name, prop.getType(), prop.getGenericType(), params, newPrefix));
        }
        return instance;
    }

    private boolean isSetter(Method method) {
        return (method.getName().startsWith("set") && method.getName().length() > 3 && method.getParameterTypes().length == 1 && (method.getModifiers() & notaccessibleMethod) == 0);
    }

    protected Object newBeanInstance() throws InstantiationException,
            IllegalAccessException {
        return beanClass.newInstance();
    }

    private void registerFields(Class clazz) {
        // recursive stop condition
        if (clazz == Object.class) {
            return;
        }
        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            if (wrappers.containsKey(field.getName())) {
                continue;
            }
            if ((field.getModifiers() & notwritableField) != 0) {
                continue;
            }
            Property w = new Property(field);
            wrappers.put(field.getName(), w);
        }
        registerFields(clazz.getSuperclass());
    }

    private void registerSetters(Class clazz) {
        if (clazz == Object.class) {
            return;
        // deep walk (superclass first)
        }
        registerSetters(clazz.getSuperclass());

        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (!isSetter(method)) {
                continue;
            }
            String propertyname = method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);
            Property wrapper = new Property(propertyname, method);
            wrappers.put(propertyname, wrapper);
        }
    }

    public static class Property {

        private Method setter;

        private Field field;

        private Class dataClass;

        private Type genericType;

        private String name;

        Property(String propertyName, Method setterMethod) {
            name = propertyName;
            setter = setterMethod;
            dataClass = setter.getParameterTypes()[0];
            genericType = setter.getGenericParameterTypes()[0];
        }

        Property(Field field) {
            this.field = field;
            this.field.setAccessible(true);
            name = field.getName();
            dataClass = field.getType();
            genericType = field.getDeclaringClass().getGenericSuperclass();
        }

        public void setValue(Object instance, Object value) {
            // due to the way the Databinder works, all properties are setted, even if not present in the request
            // here we avoid to reset all object fields
            if (value == null) {
                return;
            }
            try {
                if (setter != null) {
                    play.Logger.debug("invoke setter %s on %s with value %s", setter, instance, value);
                    setter.invoke(instance, value);
                    return;
                } else {
                    play.Logger.debug("field.set(%s, %s)", instance, value);
                    field.set(instance, value);
                }

            } catch (Exception ex) {
                play.Logger.info(ex, "ERROR in BeanWrapper when setting property %s value is %s", name, value);
            }
        }

        String getName() {
            return name;
        }

        Class getType() {
            return dataClass;
        }

        Type getGenericType() {
            return genericType;
        }
    }
}
