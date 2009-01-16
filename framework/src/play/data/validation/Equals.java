package play.data.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.sf.oval.configuration.annotation.Constraint;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(checkWith = EqualsCheck.class)
public @interface Equals {

    String message() default EqualsCheck.mes;
    String value();
}

