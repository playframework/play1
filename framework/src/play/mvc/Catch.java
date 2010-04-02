package play.mvc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark this method as @Catch interceptor
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Catch { 

    Class<?>[] value() default {};
    /**
     * Interceptor priority (0 is high priority)
     */
    int priority() default 0;

}
