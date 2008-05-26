package play.libs;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.SignaturesNamesRepository;
import play.data.binding.Binder;

public class Java {

    public static Method findPublicStaticMethod(String name, Class clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name) && Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers())) {
                return m;
            }
        }
        return null;
    }

    public static Object invokeStatic(Class clazz, String method) throws Exception {
        return invokeStatic(clazz, method, new Object[0]);
    }

    public static Object invokeStatic(Class clazz, String method, Object... args) throws Exception {
        Class[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            types[0] = args[0].getClass();
        }
        Method m = clazz.getDeclaredMethod(method, types);
        return m.invoke(null, args);
    }

    public static Object invokeStatic(Method method, Map<String, String[]> args) throws Exception {
        String[] paramsNames = SignaturesNamesRepository.get(method);
        if(paramsNames == null) {
            throw new RuntimeException("Parameter names not found");
        }
        Object[] rArgs = new Object[method.getParameterTypes().length];
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            rArgs[i] = Binder.bind(paramsNames[i], method.getParameterTypes()[i], method.getGenericParameterTypes()[i], args);
        }
        return method.invoke(null, rArgs);
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
    
    public static List<Method> findAllAnnotatedMethods(Class clazz, Class annotationType) {
        List<Method> methods = new ArrayList<Method>();
        while(!clazz.equals(Object.class)) {
            for(Method method : clazz.getDeclaredMethods()) {
                if(method.isAnnotationPresent(annotationType)) {
                    methods.add(method);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return methods;
    }
    
    
}
