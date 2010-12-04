package play.data.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.sf.oval.configuration.annotation.Constraint;

/**
 * This field must only contain letters and space.
 * Message key: validation.nospecialchars
 * $1: field name
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Constraint(checkWith = NoSpecialCharsCheck.class)
public @interface NoSpecialChars {
	String message() default NoSpecialCharsCheck.mes;
}