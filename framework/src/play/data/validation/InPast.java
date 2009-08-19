package play.data.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.sf.oval.configuration.annotation.Constraint;

/**
 * This date must be in the past.
 * Message key: validation.past
 * $1: field name
 * $2: reference date
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(checkWith = InPastCheck.class)
public @interface InPast {

    String message() default InPastCheck.mes;
    String value() default "";
}

