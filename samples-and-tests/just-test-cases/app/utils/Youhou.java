package utils;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(value={ElementType.METHOD,ElementType.TYPE})
public @interface Youhou {
    public String value() default "";
}