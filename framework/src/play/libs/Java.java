package play.libs;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Java {

    public static Method findPublicStaticMethod(String name, Class clazz) {
        for(Method m : clazz.getDeclaredMethods()) {
            if(m.getName().equals(name) && Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers())) {
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
        for(int i=0; i<args.length; i++) {
            types[0] = args[0].getClass();
        }
        Method m = clazz.getDeclaredMethod(method, types);
        return m.invoke(null, args);
    }
    
}
