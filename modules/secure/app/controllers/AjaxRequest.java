package controllers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
/**
 * Methods from a Play controller with this annotation will not be processed by the "secure" controller
 * in order to allow the user to be redirected properly after a successful login.
 */
public @interface AjaxRequest {}
