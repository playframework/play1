package play.data.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.sf.oval.configuration.annotation.Constraint;

/**
 * This date must be in the future.
 * Message key: validation.future
 * $1: field name
 * $2: reference date
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(checkWith = InFutureCheck.class)
public @interface InFuture {

    String message() default InFutureCheck.mes;
    String value() default "";
}

