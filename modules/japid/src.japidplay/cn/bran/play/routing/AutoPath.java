/**
 * 
 */
package cn.bran.play.routing;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * something like JAXRS Path   
 * @author bran
 *
 */
@java.lang.annotation.Target(value={METHOD, TYPE})
@java.lang.annotation.Retention(value=RUNTIME)
public @interface AutoPath {
	String value() default "";
}
