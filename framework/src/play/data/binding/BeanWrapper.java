package play.data.binding;

import play.Logger;
import play.classloading.enhancers.PropertiesEnhancer.PlayPropertyAccessor;
import play.exceptions.UnexpectedException;
import play.utils.Utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Parameters map to POJO binder.
 * <p/>
 * Immutable for thread safety
 */
public abstract class BeanWrapper {

    final static int notwritableField = Modifier.FINAL | Modifier.NATIVE | Modifier.STATIC;
    final static int notaccessibleMethod = Modifier.NATIVE | Modifier.STATIC;
    final static ConcurrentMap<Class<?>, BeanWrapper> beanWrapperCache = new ConcurrentHashMap<Class<?>, BeanWrapper>();


    private final Class<?> beanClass;

    /**
     * a cache for our properties and setters - really it is
     * immutable after the constructor
     */

    private final Map<String, Property> wrappers = new HashMap<String, Property>();

    private BeanWrapper(Class<?> forClass) {
        if (Logger.isTraceEnabled()) {
            Logger.trace("Bean wrapper for class %s", forClass.getName());
        }
        this.beanClass = forClass;
    }


    /**
     * Generator method - uses thread-safe cache
     *
     * @param forClass the class to be wrapped
     * @return the wrapper
     */

    public static BeanWrapper forClass(Class<?> forClass) {
        final BeanWrapper wrapper = beanWrapperCache.get(forClass);
        if (wrapper == null) {
            final BeanWrapper newWrapper = BeanWrapper.generateForClass(forClass);
            /*
              Sometimes it create an instance, but it won't be used, as
              somebody has generated another yet, and put it into the cache
            */
            final BeanWrapper cachedWrapper = beanWrapperCache.putIfAbsent(forClass, newWrapper);

            return cachedWrapper == null ? newWrapper : cachedWrapper;
            /*
            It is not thread-safe consistent to get then use put later, putIfAbsent makes it consistent,
            it returns always the same value.
            */
        }
        return wrapper;
    }

    private static BeanWrapper generateForClass(Class<?> forClass) {
        BeanWrapper beanWrapper = null;
        for (Class<?> intf : forClass.getInterfaces()) {
            if ("scala.ScalaObject".equals(intf.getName())) {
                beanWrapper = new ScalaBeanWrapper(forClass);
                break;
            }
        }

        if (beanWrapper == null) {
            beanWrapper = new JavaBeanWrapper(forClass);
        }

        beanWrapper.registerSetters(forClass);
        beanWrapper.registerFields(forClass);
        return beanWrapper;
    }

    public Collection<Property> getWrappers() {
        return wrappers.values();
    }

    public Object bind(String name, Type type, Map<String, String[]> params, String prefix, Annotation[] annotations) throws Exception {
        Object instance = newBeanInstance();
        return bind(name, type, params, prefix, instance, annotations);
    }

    public Object bind(String name, Type type, Map<String, String[]> params, String prefix, Object instance, Annotation[] annotations) throws Exception {
        for (Property prop : wrappers.values()) {
            if (Logger.isTraceEnabled()) {
                Logger.trace("beanwrapper: prefix [" + prefix + "] prop.getName() [" + prop.getName() + "]");
                for (String key : params.keySet()) {
                    Logger.trace("key: [" + key + "]");
                }
            }

            String newPrefix = prefix + "." + prop.getName();
            if (name.equals("") && prefix.equals("") && newPrefix.startsWith(".")) {
                newPrefix = newPrefix.substring(1);
            }

            if (Logger.isTraceEnabled()) {
                Logger.trace("beanwrapper: bind name [" + name + "] annotation [" + Utils.join(annotations, " ") + "]");
            }

            Object value = Binder.bindInternal(name, prop.getType(), prop.getGenericType(), prop.getAnnotations(), params, newPrefix, prop.profiles);
            if (value != Binder.MISSING) {
                if (value != Binder.NO_BINDING) {
                    prop.setValue(instance, value);
                }
            } else {
                if (Logger.isTraceEnabled()) {
                    Logger.trace("beanwrapper: bind annotation [" + Utils.join(prop.getAnnotations(), " ") + "]");
                }

                value = Binder.bindInternal(name, prop.getType(), prop.getGenericType(), annotations, params, newPrefix, prop.profiles);

                if (Logger.isTraceEnabled()) {
                    Logger.trace("beanwrapper: value [" + value + "]");
                }

                if (value != Binder.MISSING && value != Binder.NO_BINDING) {
                    prop.setValue(instance, value);
                }
            }
        }
        return instance;
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

    abstract boolean isSetter(Method method);

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
        for (Field field : getFields(clazz)) {
            if (wrappers.containsKey(field.getName())) {
                continue;
            }
            field.setAccessible(true);
            Property w = new Property(field);
            wrappers.put(field.getName(), w);
        }
        registerFields(clazz.getSuperclass());
    }

    private void registerSetters(Class<?> clazz) {
        if (clazz == Object.class) {
            return;
        }
        registerSetters(clazz.getSuperclass());

        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (!isSetter(method)) {
                continue;
            }
            final String propertyname = getPropertyName(method);
            Property wrapper = new Property(propertyname, method);
            wrappers.put(propertyname, wrapper);
        }
    }

    abstract Collection<Field> getFields(Class<?> forClass);

    abstract String getPropertyName(Method method);

    private static class JavaBeanWrapper extends BeanWrapper {
        JavaBeanWrapper(Class<?> forClass) {
            super(forClass);
        }

        @Override
        String getPropertyName(Method method) {
            return method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);
        }

        @Override
        boolean isSetter(Method method) {
            return (!method.isAnnotationPresent(PlayPropertyAccessor.class) && method.getName().startsWith("set") && method.getName().length() > 3 && method.getParameterTypes().length == 1 && (method.getModifiers() & notaccessibleMethod) == 0);
        }

        @Override
        Collection<Field> getFields(Class<?> forClass) {
            final Collection<Field> fields = new ArrayList<Field>();
            for (Field field : forClass.getFields()) {
                if ((field.getModifiers() & notwritableField) != 0) {
                    continue;
                }
                fields.add(field);
            }
            return fields;
        }
    }

    private static class ScalaBeanWrapper extends BeanWrapper {
        ScalaBeanWrapper(Class<?> forClass) {
            super(forClass);
        }

        @Override
        String getPropertyName(Method method) {
            return method.getName().substring(0, method.getName().length() - 4);
        }

        @Override
        boolean isSetter(Method method) {
            return (!method.isAnnotationPresent(PlayPropertyAccessor.class) && method.getName().endsWith("_$eq") && method.getParameterTypes().length == 1 && (method.getModifiers() & notaccessibleMethod) == 0);
        }

        @Override
        Collection<Field> getFields(Class<?> forClass) {
            return Arrays.asList(forClass.getDeclaredFields());
        }
    }

    /**
     * Immutable property wrapper
     */
    public static class Property {
        final private Annotation[] annotations;
        final private Method setter;
        final private Field field;
        final private Class<?> type;
        final private Type genericType;
        final private String name;
        final private String[] profiles;

        Property(String propertyName, Method setterMethod) {
            name = propertyName;
            setter = setterMethod;
            type = setter.getParameterTypes()[0];
            annotations = setter.getAnnotations();
            genericType = setter.getGenericParameterTypes()[0];
            field = null;
            profiles = createProfiles(this.annotations);
        }

        Property(Field field) {
            this.field = field;
            this.field.setAccessible(true);
            name = field.getName();
            type = field.getType();
            setter = null;
            annotations = field.getAnnotations();
            genericType = field.getGenericType();
            profiles = createProfiles(this.annotations);
        }

        private static String[] createProfiles(Annotation[] annotations) {
            if (annotations != null) {
                for (Annotation annotation : annotations) {
                    if (annotation.annotationType().equals(NoBinding.class)) {
                        NoBinding as = ((NoBinding) annotation);
                        return as.value();
                    }
                }
            }
            return null;
        }

        public void setValue(Object instance, Object value) {
            try {
                if (setter != null) {
                    if (Logger.isTraceEnabled()) {
                        Logger.trace("invoke setter %s on %s with value %s", setter, instance, value);
                    }
                    setter.invoke(instance, value);
                } else {
                    if (Logger.isTraceEnabled()) {
                        Logger.trace("field.set(%s, %s)", instance, value);
                    }
                    field.set(instance, value);
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
}
