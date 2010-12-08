package play.db.jpa;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(value={ElementType.METHOD,ElementType.TYPE})
public @interface Transactional {
    public boolean readOnly() default false;
}

