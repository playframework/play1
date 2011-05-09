package play.mvc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark this method as @Before interceptor
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Before {

    /**
     * Does not intercept these actions
     */
    String[] unless() default {};
    String[] only() default {};

    /**
     * Interceptor priority (0 is high priority)
     */
    int priority() default 0;

}
