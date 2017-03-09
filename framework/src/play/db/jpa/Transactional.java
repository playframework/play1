package play.db.jpa;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Transactional {
    /**
     * Db name where to persist for multi db support
     * 
     * @return The DB name
     */
    String value() default "default";

    public boolean readOnly() default false;
}
