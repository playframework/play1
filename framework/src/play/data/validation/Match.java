package play.data.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.sf.oval.configuration.annotation.Constraint;

/**
 * This field must match the regexp.
 * Message key: validation.match
 * $1: field name
 * $2: reference pattern
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(checkWith = MatchCheck.class)
public @interface Match {

    String message() default MatchCheck.mes;
    String value();
}

