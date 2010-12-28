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
@Constraint(checkWith = IPv4AddressCheck.class)
public @interface IPv4Address {
    String message() default IPv4AddressCheck.mes;
}
