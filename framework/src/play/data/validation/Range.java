package play.data.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.sf.oval.configuration.annotation.Constraint;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(checkWith = RangeCheck.class)
public @interface Range {

    String message() default RangeCheck.mes;
    double min() default Double.MIN_VALUE;
    double max() default Double.MAX_VALUE;
}

