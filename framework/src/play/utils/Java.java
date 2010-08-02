package play.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.FutureTask;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.SourceFileAttribute;
import play.Play;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.data.binding.Binder;
import play.exceptions.UnexpectedException;
import play.mvc.After;
import play.mvc.Before;
import play.mvc.Finally;
import play.mvc.With;

/**
 * Java utils
 */
public class Java {

    public static String[] extractInfosFromByteCode(byte[] code) {
        try {
            CtClass ctClass = ClassPool.getDefault().makeClass(new ByteArrayInputStream(code));
            String sourceName = ((SourceFileAttribute) ctClass.getClassFile().getAttribute("SourceFile")).getFileName();
            return new String[] {ctClass.getName(), sourceName};
        } catch (Exception e) {
            throw new UnexpectedException("Cannot read a scala generated class using javassist", e);
        }
    }

    /**
     * Try to discover what is hidden under a FutureTask (hack)
     */
    public static Object extractUnderlyingCallable(FutureTask futureTask) {
        try {
            Field syncField = FutureTask.class.getDeclaredField("sync");
            syncField.setAccessible(true);
            Object sync = syncField.get(futureTask);
            Field callableField = sync.getClass().getDeclaredField("callable");
            callableField.setAccessible(true);
            Object callable = callableField.get(sync);
            if (callable.getClass().getSimpleName().equals("RunnableAdapter")) {
                Field taskField = callable.getClass().getDeclaredField("task");
                taskField.setAccessible(true);
                return taskField.get(callable);
            }
            return callable;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Find the first public static method of a controller class
     * @param name The method name
     * @param clazz The class
     * @return The method or null
     */
    public static Method findActionMethod(String name, Class clazz) {
        while (!clazz.getName().equals("java.lang.Object")) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equalsIgnoreCase(name) && Modifier.isPublic(m.getModifiers())) {
                    // Check that it is not an intercepter
                    if (!m.isAnnotationPresent(Before.class) && !m.isAnnotationPresent(After.class) && !m.isAnnotationPresent(Finally.class)) {
                        return m;
                    }
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
    public static Object invokeStatic(Class<?> clazz, String method) throws Exception {
        return invokeStatic(clazz, method, new Object[0]);
    }

    public static Object invokeStatic(String clazz, String method) throws Exception {
        return invokeStatic(Play.classloader.loadClass(clazz), method, new Object[0]);
    }

    /**
     * Invoke a static method with args
     * @param clazz The class
     * @param method The method name
     * @param args Arguments
     * @return The result
     * @throws java.lang.Exception
     */
    public static Object invokeStatic(Class<?> clazz, String method, Object... args) throws Exception {
        Class[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i].getClass();
        }
        Method m = clazz.getDeclaredMethod(method, types);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    public static Object invokeStaticOrParent(Class<?> clazz, String method, Object... args) throws Exception {
        Class[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i].getClass();
        }
        Method m = null;
        while (!clazz.equals(Object.class) && m == null) {
            try {
                m = clazz.getDeclaredMethod(method, types);
            } catch (Exception e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (m != null) {
            m.setAccessible(true);
            if(Modifier.isStatic(m.getModifiers())) {
                return m.invoke(null, args);
            } else {
                Object instance = m.getDeclaringClass().getDeclaredField("MODULE$").get(null);
                return m.invoke(instance, args);
            }
        }
        throw new NoSuchMethodException(method);
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
            rArgs[i] = Binder.bind(paramsNames[i], method.getParameterTypes()[i], method.getGenericParameterTypes()[i], method.getParameterAnnotations()[i], args);
        }
        return rArgs;
    }

    /**
     * Retrieve parameter names of a method
     */
    public static String[] parameterNames(Method method) throws Exception {
        try {
            return (String[]) method.getDeclaringClass().getDeclaredField("$" + method.getName() + LocalVariablesNamesTracer.computeMethodHash(method.getParameterTypes())).get(null);
        } catch (Exception e) {
            throw new UnexpectedException("Cannot read parameter names for " + method);
        }
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
    public static List<Method> findAllAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotationType) {
        List<Method> methods = new ArrayList<Method>();
        // Clazz can be null if we are looking at an interface / annotation
        while (clazz != null && !clazz.equals(Object.class)) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(annotationType)) {
                    methods.add(method);
                }
            }
            if (clazz.isAnnotationPresent(With.class)) {
                for (Class withClass : clazz.getAnnotation(With.class).value()) {
                    methods.addAll(findAllAnnotatedMethods(withClass, annotationType));
                }
            }
            clazz = clazz.getSuperclass();
        }
        return methods;
    }

   /**
     * Find all annotated method from a class
     * @param classes The classes
     * @param annotationType The annotation class
     * @return A list of method object
     */
    public static List<Method> findAllAnnotatedMethods(List<Class> classes, Class<? extends Annotation> annotationType) {
        List<Method> methods = new ArrayList<Method>();
        for (Class clazz : classes) {
            methods.addAll(findAllAnnotatedMethods(clazz, annotationType));
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

    public static byte[] serialize(Object o) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(baos);
        oo.writeObject(o);
        oo.flush();
        oo.close();
        return baos.toByteArray();
    }

    public static Object deserialize(byte[] b) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        ObjectInputStream oi = new ObjectInputStream(bais);
        return oi.readObject();
    }

    /**
     * Field accessor
     * set and get value for a property, using the getter/setter when it exists or direct access otherwise.
     * final, native or static properties are safely ignored
     */
    public static class FieldWrapper {

        final static int unwritableModifiers = Modifier.FINAL | Modifier.NATIVE | Modifier.STATIC;
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
