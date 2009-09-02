package play.modules.spring;

import java.util.Map;
import play.Play;
import play.exceptions.UnexpectedException;

public class Spring {

    public static Object getBean(String name) {
        if (SpringPlugin.applicationContext == null) {
            throw new SpringException();
        }
        return SpringPlugin.applicationContext.getBean(name);
    }
    
    public static <T> T getBeanOfType(Class<T> type) {
        Map<String, Object> beans = getBeansOfType(type);
        if(beans.isEmpty()) {
            return null;
        }
        return beans.values().iterator().next();
    }
    
    public static Object getBeanOfType(String type) {
        try {
            return getBeanOfType(Play.classloader.loadClass(type));
        } catch (ClassNotFoundException ex) {
            throw new UnexpectedException(ex);
        }
    }
    
    public static <T> Map<String, T> getBeansOfType(Class type) {
        if (SpringPlugin.applicationContext == null) {
            throw new SpringException();
        }
        return SpringPlugin.applicationContext.getBeansOfType(type);
    }
    
}
