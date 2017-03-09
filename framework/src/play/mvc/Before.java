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
     * 
     * @return List of actions not to intercept
     */
    String[] unless() default {};

    /**
     * Only intercept these actions
     * 
     * @return List of actions to intercept
     */
    String[] only() default {};

    /**
     * Interceptor priority (0 is high priority)
     * 
     * @return The Interceptor priority
     */
    int priority() default 0;

}
