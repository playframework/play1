package play.inject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import javax.inject.Inject;
import play.Play;
import play.mvc.Controller;
import play.mvc.Mailer;

public class Injector {
    
    /**
     * For now, inject beans in controllers
     */
    public static void inject(BeanSource source) {
        List<Class> classes = Play.classloader.getAssignableClasses(Controller.class);
        classes.addAll(Play.classloader.getAssignableClasses(Mailer.class));
        for(Class clazz : classes) {
            for(Field field : clazz.getDeclaredFields()) {
                if(Modifier.isStatic(field.getModifiers()) && field.isAnnotationPresent(Inject.class)) {
                    Class type = field.getType();
                    field.setAccessible(true);
                    try {
                        field.set(null, source.getBeanOfType(type));
                    } catch(RuntimeException e) {
                        throw e;
                    } catch(Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

}
