package controllers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
* This annotation is used on controllers that have also specified <code>@With(Secure.class)</code>
* to specify actions which should be accessible to non-authenticated users.
*/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AllowGuest {
    String[] value();
}
