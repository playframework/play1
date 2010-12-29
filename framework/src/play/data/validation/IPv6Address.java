package play.data.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.sf.oval.configuration.annotation.Constraint;

/**
 * This field must be a valid IP address.
 * Message key: validation.ip
 * $1: field name
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Constraint(checkWith = IPv6AddressCheck.class)
public @interface IPv6Address {
    String message() default IPv6AddressCheck.mes;
}
