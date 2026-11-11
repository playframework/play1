package play.data.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This field size must be lower than.
 * Message key: validation.maxSize
 * $1: field name
 * $2: reference value
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface MaxSize {

    String message() default MaxSizeCheck.mes;
    int value();
}

