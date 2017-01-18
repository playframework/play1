package play.data.binding;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.classloading.enhancers.PropertiesEnhancer.PlayPropertyAccessor;
import play.exceptions.UnexpectedException;

/**
 * Parameters map to POJO binder.
 */
public class BeanWrapper {

    final static int notwritableField = Modifier.FINAL | Modifier.NATIVE | Modifier.STATIC;
    final static int notaccessibleMethod = Modifier.NATIVE | Modifier.STATIC;

    private Class<?> beanClass;

    /**
     * a cache for our properties and setters
     */
    private Map<String, Property> wrappers = new HashMap<String, Property>();

    public BeanWrapper(Class<?> forClass) {
        if (Logger.isTraceEnabled()) {
            Logger.trace("Bean wrapper for class %s", forClass.getName());
        }

        this.beanClass = forClass;
        boolean isScala = false;
        for (Class<?> intf : forClass.getInterfaces()) {
            if ("scala.ScalaObject".equals(intf.getName())) {
                isScala = true;
                break;
            }
        }

        registerSetters(forClass, isScala);
        if(isScala) {
            registerAllFields(forClass);
        } else {
            registerFields(forClass);
        }
    }

    public Collection<Property> getWrappers() {
        return wrappers.values();
    }

    public void set(String name, Object instance, Object value) {
        for (Property prop : wrappers.values()) {
            if (name.equals(prop.name)) {
                prop.setValue(instance, value);
                return;
            }
        }
        String message = String.format("Can't find property with name '%s' on class %s", name, instance.getClass().getName());
        Logger.warn(message);
        throw new UnexpectedException(message);

    }

    private boolean isSetter(Method method) {
        return (!method.isAnnotationPresent(PlayPropertyAccessor.class) && method.getName().startsWith("set") && method.getName().length() > 3 && method.getParameterTypes().length == 1 && (method.getModifiers() & notaccessibleMethod) == 0);
    }

    private boolean isScalaSetter(Method method) {
        return (!method.isAnnotationPresent(PlayPropertyAccessor.class) && method.getName().endsWith("_$eq") && method.getParameterTypes().length == 1 && (method.getModifiers() & notaccessibleMethod) == 0);
    }

    protected Object newBeanInstance() throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Constructor constructor = beanClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private void registerFields(Class<?> clazz) {
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

    private void registerAllFields(Class<?> clazz) {
        // recursive stop condition
        if (clazz == Object.class) {
            return;
        }
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (wrappers.containsKey(field.getName())) {
                continue;
            }
            /*if ((field.getModifiers() & notwritableField) != 0) {
                continue;
            }*/
            field.setAccessible(true);
            Property w = new Property(field);
            wrappers.put(field.getName(), w);
        }
        registerAllFields(clazz.getSuperclass());
    }

    private void registerSetters(Class<?> clazz, boolean isScala) {
        if (clazz == Object.class) {
            return;
            // deep walk (superclass first)
        }
        registerSetters(clazz.getSuperclass(), isScala);

        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            String propertyname;
            if (isScala) {
                if (!isScalaSetter(method)) {
                    continue;
                }
                propertyname = method.getName().substring(0, method.getName().length() - 4);
            } else {
                if (!isSetter(method)) {
                    continue;
                }
                propertyname = method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);
            }
            Property wrapper = new Property(propertyname, method);
            wrappers.put(propertyname, wrapper);
        }
    }

    public static class Property {

        private static final Pattern WHITESPACE = Pattern.compile("\\s+");

        private Annotation[] annotations;
        private Method setter;
        private Field field;
        private Class<?> type;
        private Type genericType;
        private String name;
        private String[] profiles;

        Property(String propertyName, Method setterMethod) {
            name = propertyName;
            setter = setterMethod;
            type = setter.getParameterTypes()[0];
            annotations = setter.getAnnotations();
            genericType = setter.getGenericParameterTypes()[0];
            setProfiles(this.annotations);
        }

        Property(Field field) {
            this.field = field;
            this.field.setAccessible(true);
            name = field.getName();
            type = field.getType();
            annotations = field.getAnnotations();
            genericType = field.getGenericType();
            setProfiles(this.annotations);
        }

        public void setProfiles(Annotation[] annotations) {
            if (annotations != null) {
                for (Annotation annotation : annotations) {
                    if (annotation.annotationType().equals(NoBinding.class)) {
                        NoBinding as = ((NoBinding) annotation);
                        profiles = as.value();
                    }
                }
            }
        }

        public String strip(Object instance, String value) {
            String result = value;

            AttributeStripping stripping = null;
            if (field == null) {
                try {
                    Field f = instance.getClass().getDeclaredField(this.name);
                    stripping = f.getAnnotation(AttributeStripping.class);
                } catch (Exception e) {
                    // we left stripping to null, which is safe
                }
            } else {
                stripping = field.getAnnotation(AttributeStripping.class);
            }

            if (stripping == null) {
                stripping = instance.getClass().getAnnotation(AttributeStripping.class);
            }

            if (stripping != null) {
                String mod = value;
                if (stripping.strip() || stripping.squish()) {
                    mod = StringUtils.strip(mod);
                }

                if (stripping.squish()) {
                    mod = WHITESPACE.matcher(mod).replaceAll(" ");
                }

                if (stripping.nullify()) {
                    mod = StringUtils.stripToNull(mod);
                }

                result = mod;

                if (Logger.isTraceEnabled()) {
                    Logger.trace("Value of attribute '%s' stripped from '%s' to '%s'", name, value, result);
                }
            }
            return result;
        }

        public void setValue(Object instance, Object value) {
            Object stripped = value; // strip(instance, value);

            try {
                if (setter != null) {
                    if (Logger.isTraceEnabled()) {
                        Logger.trace("invoke setter %s on %s with value %s", setter, instance, stripped);
                    }

                    setter.invoke(instance, stripped);
                    return;
                } else {
                    if (Logger.isTraceEnabled()) {
                        Logger.trace("field.set(%s, %s)", instance, stripped);
                    }

                    field.set(instance, stripped);
                }

            } catch (Exception ex) {
                Logger.warn(ex, "ERROR in BeanWrapper when setting property %s value is %s (%s)", name, value, value == null ? null : value.getClass());
                throw new UnexpectedException(ex);
            }
        }

        String getName() {
            return name;
        }

        Class<?> getType() {
            return type;
        }

        Type getGenericType() {
            return genericType;
        }

        Annotation[] getAnnotations() {
            return annotations;
        }

        @Override
        public String toString() {
            return type + "." + name;
        }
    }

    public Object bind(String name, Type type, Map<String, String[]> params, String prefix, Annotation[] annotations) throws Exception {
        Object instance = newBeanInstance();
        return bind(name, type, params, prefix, instance, annotations);
    }

    public Object bind(String name, Type type, Map<String, String[]> params, String prefix, Object instance, Annotation[] annotations) throws Exception {
        RootParamNode paramNode = RootParamNode.convert( params);
        // when looking at the old code in BeanBinder and Binder.bindInternal, I
        // think it is correct to use 'name+prefix'
        Binder.bindBean( paramNode.getChild(name+prefix), instance, annotations);
        return instance;
    }
}
