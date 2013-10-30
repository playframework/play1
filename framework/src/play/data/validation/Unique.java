package play.data.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.sf.oval.configuration.annotation.Constraint;
import play.db.jpa.GenericModel;

/**
 * Check that a field or or field in a context is unique.
 * You set the context as a list (comma, semicolon or space separated)
 * of properties of your {@link GenericModel}.
 *
 * Message key: validation.unique
 * $1: field name
 * $2: properties which define a context in which the column must be unique
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(checkWith = UniqueCheck.class)
public @interface Unique {
    String value() default "";
    String message() default UniqueCheck.mes;
}