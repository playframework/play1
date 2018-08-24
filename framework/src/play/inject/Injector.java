package play.inject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import play.Play;
import play.classloading.enhancers.ControllersEnhancer.ControllerSupport;
import play.jobs.Job;
import play.mvc.Mailer;

public class Injector {

    private static BeanSource beanSource = new DefaultBeanSource();

    public static void setBeanSource(BeanSource beanSource) {
        Injector.beanSource = beanSource;
    }

    public static <T> T getBeanOfType(String className) {
        try {
            return getBeanOfType((Class<T>) Class.forName(className));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot instantiate " + className, e);
        }
    }

    public static <T> T getBeanOfType(Class<T> clazz) {
        return beanSource.getBeanOfType(clazz);
    }

    /**
     * For now, inject beans in controllers and any classes that include @RequireInjection.
     * 
     * @param source
     *            the beanSource to inject
     */
    public static void inject(BeanSource source) {
        List<Class> classes = new ArrayList<>(Play.classloader.getAssignableClasses(ControllerSupport.class));
        classes.addAll(Play.classloader.getAssignableClasses(Mailer.class));
        classes.addAll(Play.classloader.getAssignableClasses(Job.class));
        classes.addAll(Play.classloader.getAnnotatedClasses(RequireInjection.class));
        for (Class<?> clazz : classes) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.isAnnotationPresent(Inject.class)) {
                    Class<?> type = field.getType();
                    field.setAccessible(true);
                    try {
                        field.set(null, source.getBeanOfType(type));
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

}
