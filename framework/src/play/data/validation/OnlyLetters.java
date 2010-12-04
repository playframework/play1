package play.data.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.sf.oval.configuration.annotation.Constraint;

/**
 * This field must only contain letters, no space or special chars.
 * Message key: validation.onlyletters
 * $1: field name
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Constraint(checkWith = OnlyLettersCheck.class)
public @interface OnlyLetters {
	String message() default OnlyLettersCheck.mes;
}