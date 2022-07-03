package play.data.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.sf.oval.configuration.annotation.Constraint;

/**
 * This field must be a valid url.
 * Message key: validation.url
 * $1: field name
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(checkWith = URLCheck.class)
public @interface URL {

    String message() default URLCheck.mes;

    /**
     * TLDs have been made mandatory so single names like "localhost" fails'
     * The default regex was built to match URLs having a real domain name (at least 2 labels separated by a dot).
     * see: https://gist.github.com/dperini/729294
     */
    boolean tldMandatory() default true;

    /**
     * exclude loopback address space
     */
    boolean excludeLoopback() default true;
}

