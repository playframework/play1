package play.data.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.sf.oval.configuration.annotation.Constraint;

/**
 * This field must be true.
 * Message key: validation.isTrue
 * $1: field name
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(checkWith = IsTrueCheck.class)
public @interface IsTrue {
    String message() default IsTrueCheck.mes;
}

