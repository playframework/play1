package play.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test annotation to control the access to MVC objects
 * 
 * @since 1.3.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface CleanTest {

    /**
     * Indicate if current MVC objects will be removed or not
     * 
     * @return true if current MVC objects will be removed, false otherwise
     */
    boolean removeCurrent() default false;

    /**
     * Indicate if current MVC objects will be created or not
     * 
     * @return true if current MVC objects will be removed, false otherwise
     */
    boolean createDefault() default false;
}