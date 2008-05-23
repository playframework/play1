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
    
}
