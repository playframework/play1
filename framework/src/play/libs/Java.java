package play.libs;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.data.binding.Binder;
import play.exceptions.UnexpectedException;
import play.mvc.Scope;

/**
 * Java utils
 */
public class Java {

    /**
     * Find the first public static method
     * @param name The method name
     * @param clazz The class
     * @return The method or null
     */
    public static Method findActionMethod(String name, Class clazz) {
        while(!clazz.getName().equals("play.mvc.Controller") && !clazz.getName().equals("java.lang.Object")) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equalsIgnoreCase(name) && Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers())) {
                    return m;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    /**
     * Invoke a static method
     * @param clazz The class
     * @param method The method name
     * @return The result
     * @throws java.lang.Exception
     */
    public static Object invokeStatic(Class clazz, String method) throws Exception {
        return invokeStatic(clazz, method, new Object[0]);
    }

    /**
     * Invoke a static method with args
     * @param clazz The class
     * @param method The method name
     * @param args Arguments
     * @return The result
     * @throws java.lang.Exception
     */
    public static Object invokeStatic(Class clazz, String method, Object... args) throws Exception {
        Class[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            types[0] = args[0].getClass();
        }
        Method m = clazz.getDeclaredMethod(method, types);
        return m.invoke(null, args);
    }

    public static Object invokeStatic(Method method, Map<String, String[]> args) throws Exception {
        return method.invoke(null, prepareArgs(method, args));
    }
    
    public static Object invokeStatic(Method method, Object[] args) throws Exception {
        return method.invoke(null, args);
    }
    
    static Object[] prepareArgs(Method method, Map<String, String[]> args) throws Exception {
        String[] paramsNames = parameterNames(method);       
        if (paramsNames == null && method.getParameterTypes().length > 0) {
            throw new UnexpectedException("Parameter names not found for method " + method);
        }
        Object[] rArgs = new Object[method.getParameterTypes().length];
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            rArgs[i] = Binder.bind(paramsNames[i], method.getParameterTypes()[i], method.getGenericParameterTypes()[i], args);
        }
        return rArgs;
    }
    
    public static String[] parameterNames(Method method) throws Exception {
        return (String[]) method.getDeclaringClass().getDeclaredField("$" + method.getName() + LocalVariablesNamesTracer.computeMethodHash(method.getParameterTypes())).get(null);
    }

    public static String rawMethodSignature(Method method) {
        StringBuilder sig = new StringBuilder();
        sig.append(method.getDeclaringClass().getName());
        sig.append(".");
        sig.append(method.getName());
        sig.append('(');
        for (Class clazz : method.getParameterTypes()) {
            sig.append(rawJavaType(clazz));
        }
        sig.append(")");
        sig.append(rawJavaType(method.getReturnType()));
        return sig.toString();
    }

    public static String rawJavaType(Class clazz) {
        if (clazz.getName().equals("void")) {
            return "V";
        }
        if (clazz.getName().equals("boolean")) {
            return "Z";
        }
        if (clazz.getName().equals("byte")) {
            return "B";
        }
        if (clazz.getName().equals("char")) {
            return "C";
        }
        if (clazz.getName().equals("double")) {
            return "D";
        }
        if (clazz.getName().equals("float")) {
            return "F";
        }
        if (clazz.getName().equals("int")) {
            return "I";
        }
        if (clazz.getName().equals("long")) {
            return "J";
        }
        if (clazz.getName().equals("short")) {
            return "S";
        }
        if (clazz.getName().startsWith("[")) {
            return clazz.getName().replace('.', '/');
        }
        return "L" + (clazz.getName().replace('.', '/')) + ";";
    }

    /**
     * Find all annotated method from a class
     * @param clazz The class
     * @param annotationType The annotation class
     * @return A list of method object
     */
    public static List<Method> findAllAnnotatedMethods(Class clazz, Class annotationType) {
        List<Method> methods = new ArrayList<Method>();
        while (!clazz.equals(Object.class)) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(annotationType)) {
                    methods.add(method);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return methods;
    }

    public static void findAllFields(Class clazz, Set<Field> found) {
        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            found.add(fields[i]);
        }
        Class sClazz = clazz.getSuperclass();
        if (sClazz != null && sClazz != Object.class) {
            findAllFields(sClazz, found);
        }
    }
    
    /** cache */
    private static Map<Field, FieldWrapper> wrappers = new HashMap<Field, FieldWrapper>();

    public static FieldWrapper getFieldWrapper(Field field) {
        if (wrappers.get(field) == null) {
            FieldWrapper fw = new FieldWrapper(field);
            play.Logger.trace("caching %s", fw);
            wrappers.put(field, fw);
        }
        return wrappers.get(field);
    }

    /**
     * Field accessor
     * set and get value for a property, using the getter/setter when it exists or direct access otherwise.
     * final, native or static properties are safely ignored
     */
    public static class FieldWrapper {

        final static int unwritableModifiers = Modifier.FINAL | Modifier.NATIVE | Modifier.STATIC;
        private Type type;
        private Method setter;
        private Method getter;
        private Field field;
        private boolean writable;
        private boolean accessible;

        private FieldWrapper(Method setter, Method getter) {
            this.setter = setter;
            this.getter = getter;
        }

        private FieldWrapper(Field field) {
            this.field = field;
            accessible = field.isAccessible();
            writable = ((field.getModifiers() & unwritableModifiers) == 0);
            String property = field.getName();
            try {
                String setterMethod = "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
                setter = field.getDeclaringClass().getMethod(setterMethod, field.getType());
            } catch (Exception ex) {
            }
            try {
                String getterMethod = "get" + property.substring(0, 1).toUpperCase() + property.substring(1);
                getter = field.getDeclaringClass().getMethod(getterMethod);
            } catch (Exception ex) {
            }
        }

        public boolean isModifiable() {
            return writable;
        }

        public void setValue(Object instance, Object value) {
            if (!writable) {
                return;
            }
            try {
                if (setter != null) {
                    play.Logger.trace("invoke setter %s on %s with value %s", setter, instance, value);
                    setter.invoke(instance, value);
                } else {
                    if (!accessible) {
                        field.setAccessible(true);
                    }
                    play.Logger.trace("field.set(%s, %s)", instance, value);
                    field.set(instance, value);
                    if (!accessible) {
                        field.setAccessible(accessible);
                    }
                }
            } catch (Exception ex) {
                play.Logger.info("ERROR: when setting value for field %s - %s", field, ex);
            }
        }

        public Object getValue(Object instance) {
            try {
                if (getter != null) {
                    return getter.invoke(instance);
                } else {
                    return field.get(instance);
                }
            } catch (Exception ex) {
                play.Logger.info("ERROR: when getting value for field %s - %s", field, ex);
            }
            return null;
        }

        @Override
        public String toString() {
            return "FieldWrapper (" + (writable ? "RW" : "R ") + ") for " + field;
        }
    }
}
