package play.utils;

import static java.util.Collections.addAll;
import static java.util.Collections.sort;
import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.FutureTask;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.SourceFileAttribute;
import play.Play;
import play.classloading.ApplicationClassloaderState;
import play.data.binding.Binder;
import play.data.binding.ParamNode;
import play.data.binding.RootParamNode;
import play.exceptions.UnexpectedException;
import play.mvc.With;

/**
 * Java utils
 */
public class Java {

    protected static JavaWithCaching _javaWithCaching = new JavaWithCaching();
    protected static ApplicationClassloaderState _lastKnownApplicationClassloaderState = Play.classloader.currentState;
    private static final Object _javaWithCachingLock = new Object();

    protected static JavaWithCaching getJavaWithCaching() {
        synchronized (_javaWithCachingLock) {
            // has the state of the ApplicationClassloader changed?
            ApplicationClassloaderState currentApplicationClassloaderState = Play.classloader.currentState;
            if (!currentApplicationClassloaderState.equals(_lastKnownApplicationClassloaderState)) {
                // it has changed.
                // we must drop our current _javaWithCaching and create a new one...
                // and start the caching over again.
                _lastKnownApplicationClassloaderState = currentApplicationClassloaderState;
                _javaWithCaching = new JavaWithCaching();

            }
            return _javaWithCaching;
        }
    }

    public static String[] extractInfosFromByteCode(byte[] code) {
        try {
            CtClass ctClass = ClassPool.getDefault().makeClass(new ByteArrayInputStream(code));
            String sourceName = ((SourceFileAttribute) ctClass.getClassFile().getAttribute("SourceFile")).getFileName();
            return new String[] { ctClass.getName(), sourceName };
        } catch (Exception e) {
            throw new UnexpectedException("Cannot read a scala generated class using javassist", e);
        }
    }

    /**
     * Try to discover what is hidden under a FutureTask (hack)
     * <p>
     * Field sync first, if not present will try field callable
     * </p>
     * 
     * @param futureTask
     *            The given tack
     * @return Field sync first, if not present will try field callable
     */
    public static Object extractUnderlyingCallable(FutureTask<?> futureTask) {
        try {
            Object callable = null;
            // Try to search for the Field sync first, if not present will try field callable
            try {
                Field syncField = FutureTask.class.getDeclaredField("sync");
                syncField.setAccessible(true);
                Object sync = syncField.get(futureTask);
                if (sync != null) {
                    Field callableField = sync.getClass().getDeclaredField("callable");
                    callableField.setAccessible(true);
                    callable = callableField.get(sync);
                }
            } catch (NoSuchFieldException ex) {
                Field callableField = FutureTask.class.getDeclaredField("callable");
                callableField.setAccessible(true);
                callable = callableField.get(futureTask);
            }
            if (callable != null && callable.getClass().getSimpleName().equals("RunnableAdapter")) {
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
     * Invoke a static method
     * 
     * @param clazz
     *            The class
     * @param method
     *            The method name
     * @return The result
     * @throws java.lang.Exception
     *             if problem occurred during invoking
     */
    public static Object invokeStatic(Class<?> clazz, String method) throws Exception {
        return invokeStatic(clazz, method, new Object[0]);
    }

    public static Object invokeStatic(String clazz, String method) throws Exception {
        return invokeStatic(Play.classloader.loadClass(clazz), method, new Object[0]);
    }

    /**
     * Invoke a static method with args
     * 
     * @param clazz
     *            The class
     * @param method
     *            The method name
     * @param args
     *            Arguments
     * @return The result
     * @throws java.lang.Exception
     *             if problem occurred during invoking
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
            if (Modifier.isStatic(m.getModifiers())) {
                return m.invoke(null, args);
            } else {
                Object instance = m.getDeclaringClass().getDeclaredField("MODULE$").get(null);
                return m.invoke(instance, args);
            }
        }
        throw new NoSuchMethodException(method);
    }

    public static Object invokeChildOrStatic(Class<?> clazz, String method, Object... args) throws Exception {
        Class invokedClass = null;
        List<Class> assignableClasses = Play.classloader.getAssignableClasses(clazz);
        if (assignableClasses.size() == 0) {
            invokedClass = clazz;
        } else {
            invokedClass = assignableClasses.get(0);
        }

        return Java.invokeStaticOrParent(invokedClass, method, args);
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

        RootParamNode rootParamNode = ParamNode.convert(args);

        Object[] rArgs = new Object[method.getParameterTypes().length];
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            rArgs[i] = Binder.bind(rootParamNode, paramsNames[i], method.getParameterTypes()[i], method.getGenericParameterTypes()[i],
                    method.getParameterAnnotations()[i]);
        }
        return rArgs;
    }

    /**
     * Retrieve parameter names of a method
     * 
     * @param method
     *            The given method
     * @return Array of parameter names
     * @throws Exception
     *             if problem occurred during invoking
     */
    public static String[] parameterNames(Method method) throws Exception {
        Parameter[] parameters = method.getParameters();
        String[] names = new String[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            names[i] = parameters[i].getName();
        }
        return names;
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
     * 
     * @param clazz
     *            The class
     * @param annotationType
     *            The annotation class
     * @return A list of method object
     */
    public static List<Method> findAllAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotationType) {
        return getJavaWithCaching().findAllAnnotatedMethods(clazz, annotationType);
    }

    /**
     * Find all annotated method from a class
     * 
     * @param classes
     *            The classes
     * @param annotationType
     *            The annotation class
     * @return A list of method object
     */
    public static List<Method> findAllAnnotatedMethods(List<Class> classes, Class<? extends Annotation> annotationType) {
        List<Method> methods = new ArrayList<>();
        for (Class clazz : classes) {
            methods.addAll(findAllAnnotatedMethods(clazz, annotationType));
        }
        return methods;
    }

    public static void findAllFields(Class clazz, Set<Field> found) {
        Field[] fields = clazz.getDeclaredFields();
        addAll(found, fields);

        Class sClazz = clazz.getSuperclass();
        if (sClazz != null && sClazz != Object.class) {
            findAllFields(sClazz, found);
        }
    }

    /** cache */
    private static Map<Field, FieldWrapper> wrappers = new HashMap<>();

    public static FieldWrapper getFieldWrapper(Field field) {
        if (wrappers.get(field) == null) {
            FieldWrapper fw = new FieldWrapper(field);
            if (play.Logger.isTraceEnabled()) {
                play.Logger.trace("caching %s", fw);
            }
            wrappers.put(field, fw);
        }
        return wrappers.get(field);
    }

    public static byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(baos);
        try {
            oo.writeObject(o);
            oo.flush();
        } finally {
            if (oo != null) {
                oo.close();
            }
        }
        return baos.toByteArray();
    }

    public static Object deserialize(byte[] b) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        try {
            ObjectInputStream oi = new ObjectInputStream(bais);
            try {
                return oi.readObject();
            } finally {
                closeQuietly(oi);
            }
        } finally {
            closeQuietly(bais);
        }
    }

    /**
     * Field accessor set and get value for a property, using the getter/setter when it exists or direct access
     * otherwise. final, native or static properties are safely ignored
     */
    public static class FieldWrapper {

        static final int unwritableModifiers = Modifier.FINAL | Modifier.NATIVE | Modifier.STATIC;
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
                    if (play.Logger.isTraceEnabled()) {
                        play.Logger.trace("invoke setter %s on %s with value %s", setter, instance, value);
                    }
                    setter.invoke(instance, value);
                } else {
                    if (!accessible) {
                        field.setAccessible(true);
                    }
                    if (play.Logger.isTraceEnabled()) {
                        play.Logger.trace("field.set(%s, %s)", instance, value);
                    }
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

/**
 * This is an internal class uses only by the Java-class. It contains functionality with caching..
 *
 * The idea is that the Java-objects creates a new instance of JavaWithCaching, each time something new is compiled..
 *
 */
class JavaWithCaching {

    /**
     * Class uses as key for storing info about the relation between a Class and an Annotation
     */
    private static class ClassAndAnnotation {
        private final Class<?> clazz;
        private final Class<? extends Annotation> annotation;

        private ClassAndAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
            this.clazz = clazz;
            this.annotation = annotation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ClassAndAnnotation that = (ClassAndAnnotation) o;

            if (annotation != null ? !annotation.equals(that.annotation) : that.annotation != null)
                return false;
            if (clazz != null ? !clazz.equals(that.clazz) : that.clazz != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = clazz != null ? clazz.hashCode() : 0;
            result = 31 * result + (annotation != null ? annotation.hashCode() : 0);
            return result;
        }
    }

    // cache follows..

    private final Object classAndAnnotationsLock = new Object();
    private final Map<ClassAndAnnotation, List<Method>> classAndAnnotation2Methods = new HashMap<>();
    private final Map<Class<?>, List<Method>> class2AllMethodsWithAnnotations = new HashMap<>();

    /**
     * Find all annotated method from a class
     * 
     * @param clazz
     *            The class
     * @param annotationType
     *            The annotation class
     * @return A list of method object
     */
    public List<Method> findAllAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotationType) {

        if (clazz == null) {
            return new ArrayList<>(0);
        }

        synchronized (classAndAnnotationsLock) {

            // first look in cache

            ClassAndAnnotation key = new ClassAndAnnotation(clazz, annotationType);

            List<Method> methods = classAndAnnotation2Methods.get(key);
            if (methods != null) {
                // cache hit
                return methods;
            }
            // have to resolve it.
            methods = new ArrayList<>();

            // get list of all annotated methods on this class..
            for (Method method : findAllAnnotatedMethods(clazz)) {
                if (method.isAnnotationPresent(annotationType)) {
                    methods.add(method);
                }
            }

            sortByPriority(methods, annotationType);

            // store it in cache
            classAndAnnotation2Methods.put(key, methods);

            return methods;
        }
    }

    private void sortByPriority(List<Method> methods, final Class<? extends Annotation> annotationType) {
        try {
            final Method priority = annotationType.getMethod("priority");
            sort(methods, new Comparator<Method>() {
                @Override
                public int compare(Method m1, Method m2) {
                    try {
                        Integer priority1 = (Integer) priority.invoke(m1.getAnnotation(annotationType));
                        Integer priority2 = (Integer) priority.invoke(m2.getAnnotation(annotationType));
                        return priority1.compareTo(priority2);
                    } catch (Exception e) {
                        // should not happen
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (NoSuchMethodException e) {
            // no need to sort - this annotation doesn't have priority() method
        }
    }

    /**
     * Find all annotated method from a class
     * 
     * @param clazz
     *            The class
     * @return A list of method object
     */
    public List<Method> findAllAnnotatedMethods(Class<?> clazz) {
        synchronized (classAndAnnotationsLock) {
            // first check the cache..
            List<Method> methods = class2AllMethodsWithAnnotations.get(clazz);
            if (methods != null) {
                // cache hit
                return methods;
            }
            // have to resolve it..
            methods = new ArrayList<>();
            // Clazz can be null if we are looking at an interface / annotation
            while (clazz != null && !clazz.equals(Object.class)) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.getAnnotations().length > 0) {
                        methods.add(method);
                    }
                }
                if (clazz.isAnnotationPresent(With.class)) {
                    for (Class withClass : clazz.getAnnotation(With.class).value()) {
                        methods.addAll(findAllAnnotatedMethods(withClass));
                    }
                }
                clazz = clazz.getSuperclass();
            }

            // store it in the cache.
            class2AllMethodsWithAnnotations.put(clazz, methods);
            return methods;
        }
    }

}
