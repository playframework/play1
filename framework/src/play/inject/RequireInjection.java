package play.inject;

import java.lang.annotation.*;

/**
 * if you want to inject beans anywhere,please implement the interface.
 *
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireInjection {
}
