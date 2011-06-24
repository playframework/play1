package play.data.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.sf.oval.configuration.annotation.Constraint;

/**
 * This field contain a phone number
 * This validation is relaxed and is intended to enforce a basic phone pattern.
 * Please implement your own @Match for country specific
 *
 * +CCC (SSSSSS)9999999999xEEEE
 *
 * + optional country code mark
 * CCC the optional country code, up to 3 digits
 * SSSSSS the optional sub-zone, up to 6 digits
 * 9999999999 the actual number, up to 20 digits (which should cover all know cases current and future)
 * x an optional extension, which can also be spelled "ext" or "extension"
 * EEEE finally an optional extension
 * space, -, . and / are all considered delimiters and can be used anywhere in the number
 * 
 * i.e.
 *  US: (305) 613 09 58 ext 101
 *  France: +33 1 47 37 62 24 x3
 *  Germany: +49-4312 / 777 777
 *  China: +86 (10)69445464
 *  UK: (020) 1234 1234
 *
 * Message key: validation.phone
 * $1: field name
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Constraint(checkWith = PhoneCheck.class)
public @interface Phone {
    String message() default PhoneCheck.mes;
}
