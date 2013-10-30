package play.data.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.sf.oval.configuration.annotation.Constraint;

/**
 * This field size must be equal or greater than.
 * Message key: validation.minSize
 * $1: field name
 * $2: reference value
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(checkWith = MinSizeCheck.class)
public @interface MinSize {

    String message() default MinSizeCheck.mes;
    int value();
}

